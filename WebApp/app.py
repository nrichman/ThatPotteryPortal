from flask import Flask
from flask import render_template
from flask import jsonify
from flask import request
from flask_cors import CORS
import MySQLdb
import cloudinary
import cloudinary.uploader

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

    order_number = data['order_num']
    order_name = data['name']
    order_phone = data['phone']
    order_email = data['email']
    order_notes = data['notes']
    order_num_items = data['num_items']    
    order_items = data['order_items']

    add_word = "INSERT INTO order_data VALUES (%s, %s, %s, %s, %s, %s, %s, NOW(), %s)"
    cur.execute(add_word, [order_number, order_name, order_phone, order_email, order_notes, 'STATUS', 'None', order_num_items])
    db.commit()

    for item in order_items.split(',')[1:]:
        add_item = "INSERT INTO order_items VALUES (%s, %s)"
        cur.execute(add_item, [order_number, item])
        db.commit()
    return 'Nice'

@app.route('/insert_items', methods=['POST'])
def insert_items():
    data = request.get_json(force=True)
    cur = db.cursor()

    order_number = data['order_num']
    order_items = data['order_items']

    for item in order_items.split(','):
        add_item = "INSERT INTO order_items VALUES (%s, %s)"
        cur.execute(add_item, [order_number, item])
        db.commit()
    return 'Nice'

@app.route('/get_image', methods=['POST'])
def get_image():
    data = request.get_json(force=True)
    image = data['image']
    result = cloudinary.uploader.upload(image)
    return result

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

@app.route('/get_order_num', methods=['GET'])
def get_order_num():
    cur = db.cursor()
    cur.execute("SELECT * FROM order_num")
    db.commit()
    data = cur.fetchall()[0][0]
    cur.execute("UPDATE order_num SET num=" + str(data + 1))
    db.commit()
    return str(data)

if __name__ == '__main__':
    app.run(debug=True)
