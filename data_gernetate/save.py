import pymysql

def save_to_mysql(csv_file_path, table_name):
    # Open database connection
    db = pymysql.connect(host="localhost", user="root", password="yby258014", db="blog", cursorclass=pymysql.cursors.DictCursor)
    cursor=db.cursor()

    # Read CSV file
    with open(csv_file_path, 'r', encoding='utf-8') as f:
        # Skip the header row
        next(f)
        # Read each row and insert into table
        for row in f:
            row = row.strip().split(',')
            if len(row)!= 5:
                continue
            insert_sql = "INSERT INTO %s (id, created_at,description,name,updated_at) VALUES ('%s', '%s', '%s', '%s', '%s')" % (table_name,row[0], row[1], row[2], row[3], row[4])
            try:
                # Execute the SQL command
                cursor.execute(insert_sql)
                # Commit changes to database
                db.commit()
            except:
                # Rollback in case there is any error
                db.rollback()


if __name__ == '__main__':
    save_to_mysql('E:\\vscode_store\\Vue_Store\\BLOG\\data_gernetate\\categories.csv', 'categories')