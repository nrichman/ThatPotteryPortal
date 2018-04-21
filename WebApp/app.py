from flask import Flask
from flask import Response
from flask import render_template
from flask import request, redirect, jsonify
from flask_cors import CORS
from flask_login import LoginManager, UserMixin, login_required, login_user, logout_user
import MySQLdb
import cloudinary
import cloudinary.uploader

app = Flask(__name__)
app.secret_key = '04957832904375894370ifdsj84mec4wfpcj43ewi89'

CORS(app)
login_manager = LoginManager()
login_manager.init_app(app)

db = MySQLdb.connect(host="pottery-db.c30pytquwht8.us-east-2.rds.amazonaws.com",
                     port=7070,
                     user="team10",
                     passwd="jenkinspottery",
                     db="pottery_data")

class User(UserMixin):

    def __init__(self, id):
        self.id = id
        self.name = "user" + str(id)
        self.password = self.name + "_secret"
        
    def __repr__(self):
        return "%d/%s/%s" % (self.id, self.name, self.password)

@login_manager.user_loader
def load_user(user_id):
    return User(user_id)


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
    return {'response': 'Nice'}, 200

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
    cur.execute("SELECT * FROM order_data")
    data = []
    for row in cur.fetchall():
        number, name, phone, email, notes, status, order_type, timestamp, num_items = row
        data.append({
            'number': number,
            'name': name,
            'phone': phone,
            'email': email,
            'notes' : notes,
            'status' : status,
            'order_type' : order_type,
            'timestamp': str(timestamp),
            'num_items': num_items,
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

@login_manager.unauthorized_handler
def unauthorized_callback():
    return redirect('/login')

@app.route('/')
@login_required
def home_page():
    cur = db.cursor()
    cur.execute("SELECT * FROM order_data")
    data = []
    for row in cur.fetchall():
        number, name, phone, email, notes, status, order_type, timestamp, num_items = row
        data.append({
            'number': number,
            'name': name,
            'phone': phone,
            'email': email,
            'notes' : notes,
            'status' : status,
            'order_type' : order_type,
            'timestamp': str(timestamp),
            'num_items': num_items,
        })

        data[-1].setdefault('urls', [])
        data[-1].setdefault('items', [])

        for num in range(1,int(num_items)+1):
            data[-1]['urls'].append('http://res.cloudinary.com/du0tdfvpl/order_' + str(number) + '_' + str(num))
        cur2 = db.cursor()
        cur2.execute('SELECT * FROM order_items WHERE order_num=' + number)
        for row in cur2.fetchall():
            a, b = row
            data[-1]['items'].append(b)
    return render_template('home.html', data=data)

@app.route('/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        username = request.form['username']
        password = request.form['password']

        cur = db.cursor()
        cur.execute("SELECT * FROM users")
        data = []
        for row in cur.fetchall():
            username_db, password_db = row

            if username == username_db and str(hash(password)) == password_db:
                id = 1
                user = User(id)
                login_user(user)
                return redirect('/')
        else:
            return redirect('/login')
    else:
        return render_template('login.html')

@app.route("/logout")
@login_required
def logout():
    logout_user()
    return redirect('/login')

if __name__ == '__main__':
    app.config['SESSION_TYPE'] = 'filesystem'
    app.debug = True
    app.run(debug=True)
