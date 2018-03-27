from flask import Flask
from flask import render_template
import MySQLdb

app = Flask(__name__)

db = MySQLdb.connect(host="pottery-db.c30pytquwht8.us-east-2.rds.amazonaws.com",
                     port=7070,
                     user="team10",
                     passwd="jenkinspottery",
                     db="pottery_data")

@app.route('/')
def users():
    cur = db.cursor()
    cur.execute("SELECT * FROM pet")
    data = []
    for row in cur.fetchall():
        data.append(row)
    return render_template('home.html', data=data)


if __name__ == '__main__':
    app.run(debug=True)
~
