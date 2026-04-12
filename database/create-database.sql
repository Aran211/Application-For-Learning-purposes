CREATE DATABASE ron_project;
CREATE USER ron_project_app WITH PASSWORD 'postgres';
GRANT ALL PRIVILEGES ON DATABASE ron_project TO ron_project_app;
