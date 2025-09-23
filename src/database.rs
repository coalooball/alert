use sqlx::PgPool;
use uuid::Uuid;
use chrono::Utc;
use bcrypt::{hash, verify, DEFAULT_COST};
use std::fs;
use crate::models::{User, UserRole, CreateUserRequest};

pub async fn create_database_pool(database_url: &str) -> Result<PgPool, sqlx::Error> {
    PgPool::connect(database_url).await
}

pub async fn init_database(base_database_url: &str, target_db_name: &str) -> Result<PgPool, Box<dyn std::error::Error + Send + Sync>> {
    // Connect to postgres database to manage databases
    let postgres_url = base_database_url.replace(&format!("/{}", target_db_name), "/postgres");
    let admin_pool = PgPool::connect(&postgres_url).await?;

    println!("🗑️  Dropping database '{}' if exists...", target_db_name);

    // Terminate all connections to the target database
    sqlx::query(&format!(
        "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '{}' AND pid <> pg_backend_pid()",
        target_db_name
    ))
    .execute(&admin_pool)
    .await?;

    // Drop the database
    sqlx::query(&format!("DROP DATABASE IF EXISTS \"{}\"", target_db_name))
        .execute(&admin_pool)
        .await?;

    println!("🔨 Creating database '{}'...", target_db_name);

    // Create the database
    sqlx::query(&format!("CREATE DATABASE \"{}\"", target_db_name))
        .execute(&admin_pool)
        .await?;

    admin_pool.close().await;

    println!("✅ Database '{}' initialized successfully", target_db_name);

    // Connect to the newly created database
    let target_pool = PgPool::connect(base_database_url).await?;
    Ok(target_pool)
}

async fn execute_sql_file(pool: &PgPool, file_path: &str) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
    let sql_content = fs::read_to_string(file_path)?;

    // Remove comments and split by semicolon
    let cleaned_sql = sql_content
        .lines()
        .filter(|line| !line.trim().starts_with("--"))
        .collect::<Vec<_>>()
        .join("\n");

    // Split by semicolon and execute each statement
    for statement in cleaned_sql.split(';') {
        let trimmed = statement.trim();
        if !trimmed.is_empty() {
            println!("  Executing: {}", trimmed.lines().next().unwrap_or("").trim());
            sqlx::query(trimmed).execute(pool).await?;
        }
    }

    Ok(())
}

pub async fn create_tables(pool: &PgPool) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
    // Get all SQL files in the sql directory and execute them in order
    let sql_files = vec![
        "sql/001_create_users_table.sql",
        "sql/002_create_users_indexes.sql",
    ];

    for file_path in sql_files {
        println!("📄 Executing SQL file: {}", file_path);
        execute_sql_file(pool, file_path).await?;
    }

    println!("✅ All database tables created successfully");
    Ok(())
}

pub async fn create_user(
    pool: &PgPool,
    request: &CreateUserRequest,
) -> Result<User, Box<dyn std::error::Error + Send + Sync>> {
    let password_hash = hash(&request.password, DEFAULT_COST)?;
    let user_id = Uuid::new_v4();
    let now = Utc::now();

    let user = sqlx::query_as::<_, User>(
        r#"
        INSERT INTO users (id, username, department, password_hash, role, created_at, updated_at)
        VALUES ($1, $2, $3, $4, $5, $6, $7)
        RETURNING *
        "#,
    )
    .bind(user_id)
    .bind(&request.username)
    .bind(&request.department)
    .bind(&password_hash)
    .bind(request.role.as_str())
    .bind(now)
    .bind(now)
    .fetch_one(pool)
    .await?;

    Ok(user)
}

pub async fn get_user_by_username(
    pool: &PgPool,
    username: &str,
) -> Result<Option<User>, sqlx::Error> {
    let user = sqlx::query_as::<_, User>(
        "SELECT * FROM users WHERE username = $1"
    )
    .bind(username)
    .fetch_optional(pool)
    .await?;

    Ok(user)
}

pub async fn verify_user_password(
    pool: &PgPool,
    username: &str,
    password: &str,
) -> Result<Option<User>, Box<dyn std::error::Error + Send + Sync>> {
    if let Some(user) = get_user_by_username(pool, username).await? {
        if verify(password, &user.password_hash)? {
            Ok(Some(user))
        } else {
            Ok(None)
        }
    } else {
        Ok(None)
    }
}

pub async fn get_all_users(pool: &PgPool) -> Result<Vec<User>, sqlx::Error> {
    let users = sqlx::query_as::<_, User>(
        "SELECT * FROM users ORDER BY created_at DESC"
    )
    .fetch_all(pool)
    .await?;

    Ok(users)
}

pub async fn delete_user(pool: &PgPool, user_id: Uuid) -> Result<bool, sqlx::Error> {
    let result = sqlx::query("DELETE FROM users WHERE id = $1")
        .bind(user_id)
        .execute(pool)
        .await?;

    Ok(result.rows_affected() > 0)
}

pub async fn init_admin_user(pool: &PgPool) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
    // Check if admin user already exists
    if let Some(_) = get_user_by_username(pool, "admin").await? {
        return Ok(());
    }

    // Create default admin user
    let admin_request = CreateUserRequest {
        username: "admin".to_string(),
        department: "系统管理部".to_string(),
        password: "admin123".to_string(),
        role: UserRole::Admin,
    };

    create_user(pool, &admin_request).await?;
    println!("✅ Default admin user created: admin/admin123");

    Ok(())
}