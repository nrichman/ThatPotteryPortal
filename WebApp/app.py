from flask import Flask
from flask import render_template
from flask import jsonify
from flask import request
from flask_cors import CORS
import MySQLdb

app = Flask(__name__)
CORS(app)

db = MySQLdb.connect(host="pottery-db.c30pytquwht8.us-east-2.rds.amazonaws.com",
                     port=7070,
                     user="team10",
                     passwd="jenkinspottery",
                     db="pottery_data")

@app.route('/test_write', methods=['POST'])
def test_write():
    """Inserts a single word to test table in DB
    """
    data = request.get_json(force=True)
    cur = db.cursor()

    word = data['word']

    add_word = "INSERT INTO words VALUES (%s)"
    cur.execute(add_word, [word])
    db.commit()
    return 'Nice'

@app.route('/insert_order', methods=['POST'])
def insert_order():
    """Inserts an entire JSON order into DB
    """
    data = request.get_json(force=True)
    cur = db.cursor()

    order_number = data['number']
    order_name = data['name']
    order_phone = data['phone']
    order_email = data['email']
    #order_timestamp = data['timestamp']
    order_notes = data['notes']
    order_status = data['status']


    add_word = "INSERT INTO orders VALUES (%s, %s, %s, %s, NOW(), %s, %s)"
    cur.execute(add_word, [order_number, order_name, order_phone, order_email, order_notes, order_status])
    db.commit()
    return 'Nice'


@app.route('/get_orders', methods=['GET'])
def get_orders():
    """Gets all orders from the DB
    """
    cur = db.cursor()
    cur.execute("SELECT * FROM orders")
    data = []
    for row in cur.fetchall():
        number, name, phone, email, timestamp, notes, status = row
        data.append({
            'number': number,
            'name': name,
            'phone': str(phone),
            'email': email,
            'timestamp': str(timestamp),
            'notes': notes,
            'status': status
        })
    return jsonify({'orders': data})
    #return render_template('home.html', data=data)


if __name__ == '__main__':
    app.run(debug=True)
