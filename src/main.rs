use axum::{middleware, routing::{get, post, delete}, Router};
use clap::Parser;
use std::net::SocketAddr;
use tower::ServiceBuilder;
use tower_http::{
    cors::CorsLayer,
    services::{ServeDir, ServeFile},
};

mod models;
mod database;
mod auth;
mod handlers;

use database::{create_database_pool, create_tables, init_admin_user, init_database};
use auth::{auth_middleware, admin_middleware};
use handlers::{
    login_handler, logout_handler, me_handler,
    create_user_handler, get_users_handler, delete_user_handler,
    system_info_handler
};

#[derive(Parser)]
#[command(name = "alerts")]
#[command(about = "Alert System Web Server")]
#[command(version = "0.1.0")]
struct Args {
    #[arg(short = 'P', long, default_value_t = 3000)]
    #[arg(help = "Port to bind the server to")]
    port: u16,

    #[arg(short = 'H', long, default_value = "127.0.0.1")]
    #[arg(help = "Host address to bind the server to")]
    host: String,

    #[arg(long, default_value = "postgresql://admin:postgres123@localhost:5432/alert_system")]
    #[arg(help = "Database connection URL")]
    database_url: String,

    #[arg(long, help = "Initialize database (drop and recreate)")]
    init_db: bool,
}

#[tokio::main]
async fn main() {
    let args = Args::parse();

    // Initialize database
    let pool = if args.init_db {
        println!("🔄 Initializing database (drop and recreate)...");
        match init_database(&args.database_url, "alert_system").await {
            Ok(pool) => pool,
            Err(e) => {
                eprintln!("❌ Failed to initialize database: {}", e);
                eprintln!("💡 Make sure PostgreSQL is running with: docker-compose -f docker-compose/postgres-compose.yml up -d");
                std::process::exit(1);
            }
        }
    } else {
        match create_database_pool(&args.database_url).await {
            Ok(pool) => {
                println!("✅ Database connected");
                pool
            }
            Err(e) => {
                eprintln!("❌ Failed to connect to database: {}", e);
                eprintln!("💡 Make sure PostgreSQL is running with: docker-compose -f docker-compose/postgres-compose.yml up -d");
                eprintln!("💡 Or use --init-db flag to initialize the database");
                std::process::exit(1);
            }
        }
    };

    // Create tables
    if let Err(e) = create_tables(&pool).await {
        eprintln!("❌ Failed to create tables: {}", e);
        std::process::exit(1);
    }

    // Initialize admin user
    if let Err(e) = init_admin_user(&pool).await {
        eprintln!("❌ Failed to initialize admin user: {}", e);
        std::process::exit(1);
    }

    // If only initializing database, exit here
    if args.init_db {
        println!("🎉 Database initialization completed successfully!");
        println!("💡 You can now run 'cargo run' to start the server");
        std::process::exit(0);
    }

    // Public API routes
    let public_routes = Router::new()
        .route("/login", post(login_handler));

    // Protected API routes (require authentication)
    let protected_routes = Router::new()
        .route("/logout", post(logout_handler))
        .route("/me", get(me_handler))
        .route("/system-info", get(system_info_handler))
        .route_layer(middleware::from_fn(auth_middleware));

    // Admin API routes (require admin role)
    let admin_routes = Router::new()
        .route("/users", get(get_users_handler))
        .route("/users", post(create_user_handler))
        .route("/users/:id", delete(delete_user_handler))
        .route_layer(middleware::from_fn(admin_middleware));

    let api_routes = Router::new()
        .merge(public_routes)
        .merge(protected_routes)
        .merge(admin_routes)
        .with_state(pool);

    // Main app router
    let app = Router::new()
        .nest("/api", api_routes)
        .nest_service(
            "/",
            ServeDir::new("frontend/dist")
                .not_found_service(ServeFile::new("frontend/dist/index.html")),
        )
        .layer(ServiceBuilder::new().layer(CorsLayer::permissive()));

    let addr: SocketAddr = format!("{}:{}", args.host, args.port)
        .parse()
        .expect("Invalid host or port");

    println!("🚀 Server running on http://{}", addr);
    println!("📱 Alert System Frontend available at http://{}", addr);
    println!("🔑 API endpoints available at http://{}/api", addr);
    println!("👤 Default admin: admin/admin123");
    println!("💡 Host: {} | Port: {}", args.host, args.port);

    let listener = tokio::net::TcpListener::bind(addr).await.unwrap();
    axum::serve(listener, app).await.unwrap();
}
