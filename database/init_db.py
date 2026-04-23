import pymysql
import sys
import os

def init_database():
    db_config = {
        'host': 'localhost',
        'port': 3306,
        'user': 'root',
        'password': 'qazwsxasd',
        'charset': 'utf8mb4'
    }
    
    script_dir = os.path.dirname(os.path.abspath(__file__))
    sql_file = os.path.join(script_dir, 'init.sql')
    
    print("=" * 50)
    print("初始化道路病害检测系统数据库")
    print("=" * 50)
    print()
    
    try:
        print("正在连接MySQL服务器...")
        conn = pymysql.connect(**db_config)
        cursor = conn.cursor()
        
        print("正在读取SQL脚本...")
        with open(sql_file, 'r', encoding='utf-8') as f:
            sql_content = f.read()
        
        sql_statements = sql_content.split(';')
        
        print("正在执行SQL脚本...")
        for i, statement in enumerate(sql_statements):
            statement = statement.strip()
            if statement:
                try:
                    cursor.execute(statement)
                except Exception as e:
                    if 'Duplicate' not in str(e):
                        print(f"警告: {str(e)[:100]}")
        
        conn.commit()
        cursor.close()
        conn.close()
        
        print()
        print("=" * 50)
        print("数据库初始化完成!")
        print("数据库名: road_inspection")
        print("=" * 50)
        
        return True
        
    except Exception as e:
        print(f"错误: {e}")
        return False

if __name__ == "__main__":
    success = init_database()
    sys.exit(0 if success else 1)
