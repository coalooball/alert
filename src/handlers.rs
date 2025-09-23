use axum::{
    extract::{Extension, Path, State},
    http::StatusCode,
    response::Json,
};
use axum_extra::extract::cookie::{Cookie, CookieJar, SameSite};
use serde_json::{json, Value};
use sqlx::PgPool;
use uuid::Uuid;
use sysinfo::{System, SystemExt};
use chrono::Utc;

use crate::{
    auth::create_jwt_token,
    database::{create_user, delete_user, get_all_users, verify_user_password},
    models::{Claims, CreateUserRequest, LoginRequest, LoginResponse, UserResponse, SystemInfo},
};

pub async fn login_handler(
    State(pool): State<PgPool>,
    Json(request): Json<LoginRequest>,
) -> Result<(CookieJar, Json<LoginResponse>), (StatusCode, Json<Value>)> {
    match verify_user_password(&pool, &request.username, &request.password).await {
        Ok(Some(user)) => {
            let token = create_jwt_token(&user).map_err(|_| {
                (
                    StatusCode::INTERNAL_SERVER_ERROR,
                    Json(json!({"error": "Failed to create token"})),
                )
            })?;

            let user_response = UserResponse::from(user);
            let response = LoginResponse {
                token: token.clone(),
                user: user_response,
            };

            // Set JWT token in cookie
            let cookie = Cookie::build(("token", token))
                .http_only(true)
                .same_site(SameSite::Lax)
                .path("/")
                .max_age(time::Duration::hours(24))
                .build();

            let jar = CookieJar::new().add(cookie);

            Ok((jar, Json(response)))
        }
        Ok(None) => Err((
            StatusCode::UNAUTHORIZED,
            Json(json!({"error": "Invalid username or password"})),
        )),
        Err(_) => Err((
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(json!({"error": "Database error"})),
        )),
    }
}

pub async fn logout_handler() -> CookieJar {
    let cookie = Cookie::build(("token", ""))
        .http_only(true)
        .same_site(SameSite::Lax)
        .path("/")
        .max_age(time::Duration::seconds(0))
        .build();

    CookieJar::new().add(cookie)
}

pub async fn me_handler(
    Extension(claims): Extension<Claims>,
) -> Result<Json<Value>, (StatusCode, Json<Value>)> {
    Ok(Json(json!({
        "id": claims.sub,
        "username": claims.username,
        "role": claims.role
    })))
}

pub async fn create_user_handler(
    State(pool): State<PgPool>,
    Extension(claims): Extension<Claims>,
    Json(request): Json<CreateUserRequest>,
) -> Result<Json<UserResponse>, (StatusCode, Json<Value>)> {
    // Only admins can create users
    if claims.role != "admin" {
        return Err((
            StatusCode::FORBIDDEN,
            Json(json!({"error": "Only admins can create users"})),
        ));
    }

    match create_user(&pool, &request).await {
        Ok(user) => Ok(Json(UserResponse::from(user))),
        Err(e) => {
            if e.to_string().contains("duplicate key") {
                Err((
                    StatusCode::CONFLICT,
                    Json(json!({"error": "Username already exists"})),
                ))
            } else {
                Err((
                    StatusCode::INTERNAL_SERVER_ERROR,
                    Json(json!({"error": "Failed to create user"})),
                ))
            }
        }
    }
}

pub async fn get_users_handler(
    State(pool): State<PgPool>,
    Extension(claims): Extension<Claims>,
) -> Result<Json<Vec<UserResponse>>, (StatusCode, Json<Value>)> {
    // Only admins can view all users
    if claims.role != "admin" {
        return Err((
            StatusCode::FORBIDDEN,
            Json(json!({"error": "Only admins can view users"})),
        ));
    }

    match get_all_users(&pool).await {
        Ok(users) => {
            let user_responses: Vec<UserResponse> = users.into_iter().map(UserResponse::from).collect();
            Ok(Json(user_responses))
        }
        Err(_) => Err((
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(json!({"error": "Failed to fetch users"})),
        )),
    }
}

pub async fn delete_user_handler(
    State(pool): State<PgPool>,
    Extension(claims): Extension<Claims>,
    Path(user_id): Path<Uuid>,
) -> Result<Json<Value>, (StatusCode, Json<Value>)> {
    // Only admins can delete users
    if claims.role != "admin" {
        return Err((
            StatusCode::FORBIDDEN,
            Json(json!({"error": "Only admins can delete users"})),
        ));
    }

    // Prevent admin from deleting themselves
    if claims.sub == user_id.to_string() {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(json!({"error": "Cannot delete your own account"})),
        ));
    }

    match delete_user(&pool, user_id).await {
        Ok(true) => Ok(Json(json!({"message": "User deleted successfully"}))),
        Ok(false) => Err((
            StatusCode::NOT_FOUND,
            Json(json!({"error": "User not found"})),
        )),
        Err(_) => Err((
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(json!({"error": "Failed to delete user"})),
        )),
    }
}

pub async fn system_info_handler(
    State(pool): State<PgPool>,
    Extension(_claims): Extension<Claims>,
) -> Result<Json<SystemInfo>, (StatusCode, Json<Value>)> {
    let mut sys = System::new_all();
    sys.refresh_all();

    let system_name = sys.name().unwrap_or_else(|| "Unknown".to_string());
    let system_version = sys.long_os_version().unwrap_or_else(|| "Unknown".to_string());
    let kernel_version = sys.kernel_version().unwrap_or_else(|| "Unknown".to_string());
    let hostname = sys.host_name().unwrap_or_else(|| "Unknown".to_string());
    let architecture = std::env::consts::ARCH.to_string();

    let cpu_cores = sys.cpus().len();
    let total_memory = sys.total_memory();
    let available_memory = sys.available_memory();

    let boot_time = sys.boot_time();
    let uptime = sys.uptime();

    let current_time = Utc::now().to_rfc3339();
    let app_version = env!("CARGO_PKG_VERSION").to_string();

    let database_connected = pool.acquire().await.is_ok();

    let server_address = std::env::var("SERVER_ADDRESS").unwrap_or_else(|_| "127.0.0.1".to_string());
    let server_port: u16 = std::env::var("SERVER_PORT")
        .unwrap_or_else(|_| "3000".to_string())
        .parse()
        .unwrap_or(3000);

    let system_info = SystemInfo {
        system_name,
        system_version,
        kernel_version,
        hostname,
        architecture,
        cpu_cores,
        total_memory,
        available_memory,
        boot_time,
        uptime,
        current_time,
        app_version,
        database_connected,
        server_address,
        server_port,
    };

    Ok(Json(system_info))
}