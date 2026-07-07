CREATE TABLE IF NOT EXISTS folders (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    path TEXT NOT NULL UNIQUE,
    status TEXT NOT NULL DEFAULT 'PENDING',
    last_indexed TEXT,
    include_hidden INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%S','now')),
    updated_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%S','now'))
)
@@

CREATE TABLE IF NOT EXISTS indexed_files (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    folder_id INTEGER NOT NULL,
    file_path TEXT NOT NULL,
    file_name TEXT NOT NULL,
    extension TEXT NOT NULL DEFAULT '',
    size_bytes INTEGER NOT NULL DEFAULT 0,
    last_modified TEXT NOT NULL,
    content_snippet TEXT,
    full_content TEXT,
    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%S','now')),
    FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE CASCADE
)
@@

CREATE INDEX IF NOT EXISTS idx_files_name
ON indexed_files(file_name)
@@

CREATE INDEX IF NOT EXISTS idx_files_ext
ON indexed_files(extension)
@@

CREATE VIRTUAL TABLE IF NOT EXISTS files_fts USING fts5(
    file_name,
    extension,
    content_snippet,
    full_content,
    content='indexed_files',
    content_rowid='id'
)
@@

CREATE TRIGGER IF NOT EXISTS files_ai
AFTER INSERT ON indexed_files
BEGIN
    INSERT INTO files_fts(rowid, file_name, extension, content_snippet, full_content)
    VALUES (new.id, new.file_name, new.extension, new.content_snippet, new.full_content);
END
@@

CREATE TRIGGER IF NOT EXISTS files_ad
AFTER DELETE ON indexed_files
BEGIN
    INSERT INTO files_fts(files_fts, rowid, file_name, extension, content_snippet, full_content)
    VALUES('delete', old.id, old.file_name, old.extension, old.content_snippet, old.full_content);
END
@@

CREATE TRIGGER IF NOT EXISTS files_au
AFTER UPDATE ON indexed_files
BEGIN
    INSERT INTO files_fts(files_fts, rowid, file_name, extension, content_snippet, full_content)
    VALUES('delete', old.id, old.file_name, old.extension, old.content_snippet, old.full_content);
    INSERT INTO files_fts(rowid, file_name, extension, content_snippet, full_content)
    VALUES (new.id, new.file_name, new.extension, new.content_snippet, new.full_content);
END
@@

CREATE TABLE IF NOT EXISTS search_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    query_text TEXT NOT NULL,
    extension_filter TEXT,
    date_from TEXT,
    date_to TEXT,
    result_count INTEGER DEFAULT 0,
    searched_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%S','now'))
)
@@

CREATE INDEX IF NOT EXISTS idx_search_date
ON search_history(searched_at)
@@

CREATE TABLE IF NOT EXISTS file_open_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_path TEXT NOT NULL,
    opened_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%S','now'))
)
@@

CREATE INDEX IF NOT EXISTS idx_open_date
ON file_open_history(opened_at)
 @@