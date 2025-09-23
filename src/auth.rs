use axum::{
    extract::Request,
    http::{header::AUTHORIZATION, HeaderMap, StatusCode},
    middleware::Next,
    response::Response,
};
use axum_extra::extract::cookie::CookieJar;
use jsonwebtoken::{decode, encode, DecodingKey, EncodingKey, Header, Validation};
use chrono::{Duration, Utc};
use crate::models::{Claims, User, UserRole};

pub const JWT_SECRET: &[u8] = b"your-secret-key-change-this-in-production";
pub const TOKEN_DURATION_HOURS: i64 = 24;

pub fn create_jwt_token(user: &User) -> Result<String, jsonwebtoken::errors::Error> {
    let expiration = Utc::now()
        .checked_add_signed(Duration::hours(TOKEN_DURATION_HOURS))
        .expect("valid timestamp")
        .timestamp() as usize;

    let claims = Claims {
        sub: user.id.to_string(),
        username: user.username.clone(),
        role: user.role.clone(),
        exp: expiration,
    };

    encode(&Header::default(), &claims, &EncodingKey::from_secret(JWT_SECRET))
}

pub fn verify_jwt_token(token: &str) -> Result<Claims, jsonwebtoken::errors::Error> {
    decode::<Claims>(
        token,
        &DecodingKey::from_secret(JWT_SECRET),
        &Validation::default(),
    )
    .map(|data| data.claims)
}

pub async fn auth_middleware(
    headers: HeaderMap,
    cookies: CookieJar,
    mut request: Request,
    next: Next,
) -> Result<Response, StatusCode> {
    // Try to get token from Authorization header first
    let token = if let Some(auth_header) = headers.get(AUTHORIZATION) {
        auth_header
            .to_str()
            .map_err(|_| StatusCode::UNAUTHORIZED)?
            .strip_prefix("Bearer ")
            .ok_or(StatusCode::UNAUTHORIZED)?
    } else if let Some(cookie) = cookies.get("token") {
        // Fallback to cookie
        cookie.value()
    } else {
        return Err(StatusCode::UNAUTHORIZED);
    };

    let claims = verify_jwt_token(token).map_err(|_| StatusCode::UNAUTHORIZED)?;

    // Add claims to request extensions for use in handlers
    request.extensions_mut().insert(claims);

    Ok(next.run(request).await)
}

pub async fn admin_middleware(
    headers: HeaderMap,
    cookies: CookieJar,
    mut request: Request,
    next: Next,
) -> Result<Response, StatusCode> {
    // First run auth middleware
    let token = if let Some(auth_header) = headers.get(AUTHORIZATION) {
        auth_header
            .to_str()
            .map_err(|_| StatusCode::UNAUTHORIZED)?
            .strip_prefix("Bearer ")
            .ok_or(StatusCode::UNAUTHORIZED)?
    } else if let Some(cookie) = cookies.get("token") {
        cookie.value()
    } else {
        return Err(StatusCode::UNAUTHORIZED);
    };

    let claims = verify_jwt_token(token).map_err(|_| StatusCode::UNAUTHORIZED)?;

    // Check if user is admin
    if claims.role != UserRole::Admin.as_str() {
        return Err(StatusCode::FORBIDDEN);
    }

    request.extensions_mut().insert(claims);
    Ok(next.run(request).await)
}