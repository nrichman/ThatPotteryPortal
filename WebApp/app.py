from flask import Flask
from flask import render_template
from flask import jsonify
import MySQLdb

app = Flask(__name__)

db = MySQLdb.connect(host="pottery-db.c30pytquwht8.us-east-2.rds.amazonaws.com",
                     port=7070,
                     user="team10",
                     passwd="jenkinspottery",
                     db="pottery_data")


def get_posts():
    cur = db.cursor()
    cur.execute("SELECT * FROM pet")
    data = []
    for row in cur.fetchall():
        user_name, pet_name, date_time, image_url = row
        data.append({
            'user': user_name,
            'pet': pet_name,
            'time': str(date_time),
            'image': image_url
        })
    return data

@app.route('/test')
def users():
    cur = db.cursor()
    cur.execute("SELECT * FROM pet")
    data = []
    for row in cur.fetchall():
        data.append(row)
    return jsonify({'orders': get_posts()})
    #return render_template('home.html', data=data)


if __name__ == '__main__':
    #app.run(debug=True)
    get_posts()