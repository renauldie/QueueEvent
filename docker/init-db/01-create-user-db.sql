SELECT 'CREATE DATABASE event_queue_user_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'event_queue_user_db')\gexec
