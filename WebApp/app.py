from flask import Flask
from flask import Response
from flask import render_template
from flask import request, redirect, jsonify
from flask_cors import CORS
from flask_login import LoginManager, UserMixin, login_required, login_user, logout_user
import MySQLdb
import cloudinary
import cloudinary.uploader
import hashlib
import json
from datetime import datetime, timedelta

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
    """Loads the user for flask login
    """
    return User(user_id)


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

    date_now = datetime.now().strftime('%Y-%m-%d %H:%M:%S')

    add_word = "INSERT INTO order_data VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)"
    cur.execute(add_word, [order_number, order_name, order_phone, order_email, order_notes, 'Ready', 'None', date_now, order_num_items])
    db.commit()

    for item in order_items.split('+')[1:]:
        thing, signature = item.split('=')
        add_item = "INSERT INTO good_items VALUES (%s, %s, %s)"
        cur.execute(add_item, [thing, signature, order_number])
        db.commit()
    
    resp = {'status'  : 'Nice'}
    resp = json.dumps(resp)
    resp = Response(resp, status=200, mimetype='application/json')
    return resp


@app.route('/get_order_num', methods=['GET'])
def get_order_num():
    """Returns the next order number
    """
    cur = db.cursor()
    cur.execute("SELECT * FROM order_num")
    db.commit()
    data = cur.fetchall()[0][0]
    cur.execute("UPDATE order_num SET num=" + str(data + 1))
    db.commit()
    return str(data)


@login_manager.unauthorized_handler
def unauthorized_callback():
    """If login is required, redirect to login
    """
    return redirect('/login')


@app.route('/', methods=['GET','POST'])
@login_required
def home_page():
    """Pull all data from DB and render the main page
    """
    cur = db.cursor()
    data = []

    if request.method == "GET":
        cur.execute("SELECT * FROM order_data where status=\'Ready\'")
        db.commit()
    else:
        cur.execute("SELECT * FROM order_data")
        db.commit()

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
        data[-1].setdefault('signatures', [])

        for num in range(1,int(num_items)+1):
            data[-1]['urls'].append('http://res.cloudinary.com/du0tdfvpl/order_' + str(number) + '_' + str(num))
        cur2 = db.cursor()
        cur2.execute('SELECT * FROM good_items WHERE url=' + number)
        db.commit()
        for row in cur2.fetchall():
            item, sig, num = row
            data[-1]['items'].append(item)
            data[-1]['signatures'].append(sig)

    if request.method == 'POST':
        return render_template('home.html', data=data, toggle=True)

    return render_template('home.html', data=data, toggle=False)

@app.route('/login', methods=['GET', 'POST'])
def login():
    """Render the login page
    """
    if request.method == 'POST':
        username = request.form['username']
        password = request.form['password']

        cur = db.cursor()
        cur.execute("SELECT * FROM good_users")
        db.commit()
        data = []
        for row in cur.fetchall():
            username_db, password_db = row

            if username == username_db and hashlib.sha224(password.encode('utf-8')).hexdigest() == password_db:
                id = 1
                user = User(id)
                login_user(user)
                return redirect('/')
        else:
            return redirect('/login')
    else:
        return render_template('login.html')

@app.route("/charts")
def chart_page():
    cur = db.cursor()
    cur.execute("SELECT item FROM good_items")
    db.commit()

    # Generate most popular item data
    item_list = []
    for row in cur.fetchall():
        item_list.append(row[0].strip())
    item_list = sorted([(item_list.count(x),x) for x in set(item_list)], reverse=True)
    bar_values = [x[0] for x in item_list[:8]]
    bar_labels = [x[1] for x in item_list[:8]]

    # Generate days of week data 
    cur.execute("SELECT timestamp, num_items FROM order_data")
    db.commit()


    day_values = [0] * 7
    day_labels = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']

    date_map = {}
    for i in range(7,0,-1):
        date_map.setdefault((datetime.today() - timedelta(days=i)).strftime('%b %d'), 0)
    for row in cur.fetchall():
        timestamp, num_items = row
        if timestamp.strftime('%b %d') in date_map:
            date_map[timestamp.strftime('%b %d')] += 1

        day_values[day_labels.index(timestamp.strftime('%a'))] += 1

    max_weekly_val = 0
    week_values = []
    week_labels = sorted(date_map.iterkeys())

    for label in week_labels:
        if date_map[label] > max_weekly_val:
            max_weekly_val = date_map[label]
        week_values.append(date_map[label])


    legend = 'Monthly Data'
    labels = ["January", "February", "March", "April", "May", "June", "July", "August"]
    values = [10000, 9000, 8000, 7000, 6000, 4000, 7000, 8000]
    return render_template('charts.html', week_labels=week_labels, week_values=week_values, 
                            max_weekly_val=max_weekly_val, bar_labels=bar_labels, bar_values=bar_values,
                            day_labels=day_labels, day_values=day_values)

@app.route("/update_order", methods=['POST', 'OPTIONS'])
def update_order():
    """Toggle a user order
    """
    if request.method == 'OPTIONS':
        headers = {
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'POST, GET, OPTIONS',
            'Access-Control-Max-Age': 1000,
            'Access-Control-Allow-Headers': 'origin, x-csrftoken, content-type, accept',
        }
        return '', 200, headers
    data = json.loads(request.data)

    toggle = data['toggle']
    order_num = data['order_num']

    cur = db.cursor()
    if toggle:
        cur.execute("UPDATE order_data SET status='Ready' where order_num={}".format(str(order_num)))
        db.commit()
    else:
        cur.execute("UPDATE order_data SET status='Picked Up' where order_num={}".format(str(order_num)))
        db.commit()

    resp = {'status'  : 'Nice'}
    resp = json.dumps(resp)
    resp = Response(resp, status=200, mimetype='application/json')
    return resp

@app.route("/logout")
@login_required
def logout():
    """User logout
    """
    logout_user()
    return redirect('/login')


if __name__ == '__main__':
    app.config['SESSION_TYPE'] = 'filesystem'
    app.debug = True
    app.run(debug=True)
