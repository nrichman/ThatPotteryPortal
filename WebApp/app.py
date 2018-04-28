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

    Retrieves a JSON from the mobile application. JSON is expected
    to contain most of the information in a customer order. 
    Both the order_data and good_items tables will be updated
    with the corresponding data.
    """
    cur = db.cursor()

    # Parses the JSON from the request
    data = request.get_json(force=True)
    order_number = data['order_num']
    order_name = data['name']
    order_phone = data['phone']
    order_email = data['email']
    order_notes = data['notes']
    order_num_items = data['num_items']    
    order_items = data['order_items']

    date_now = datetime.now().strftime('%Y-%m-%d %H:%M:%S')

    # Adds the overall order into the order_data table
    add_word = "INSERT INTO order_data VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)"
    cur.execute(add_word, [order_number, order_name, order_phone, order_email, order_notes, 'Ready', 'None', date_now, order_num_items])
    db.commit()

    # Loops through each item in the order and adds them separately into the good_items table
    for item in order_items.split('+')[1:]:
        thing, signature = item.split('=')
        add_item = "INSERT INTO good_items VALUES (%s, %s, %s)"
        cur.execute(add_item, [thing, signature, order_number])
        db.commit()
    
    # Returns response code
    resp = {'status'  : 'Nice'}
    resp = json.dumps(resp)
    resp = Response(resp, status=200, mimetype='application/json')
    return resp


@app.route('/get_order_num', methods=['GET'])
def get_order_num():
    """Returns the next order number

    Each order has an associated order number to build
    Cloudinary URLs. The oder_num database keeps track of the 
    current order number. This function sends that data to the
    mobile application and increments the order number.
    """
    cur = db.cursor()
    cur.execute("SELECT * FROM order_num")
    db.commit()
    data = cur.fetchall()[0][0]
    cur.execute("UPDATE order_num SET num=" + str(data + 1))
    db.commit()
    return str(data)


@app.route("/update_order", methods=['POST', 'OPTIONS'])
def update_order():
    """Toggle a user order as ready or picked up

    When the user clicks the 'Ready' or 'Picked Up' button on the page
    this function is envoked by the Javascript. By updating the status of the
    item in the database we can load different items based on whether the user
    is in the 'Display All' or 'Display Ready' view.
    """
    
    if request.method == 'OPTIONS':
        headers = {
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'POST, GET, OPTIONS',
            'Access-Control-Max-Age': 1000,
            'Access-Control-Allow-Headers': 'origin, x-csrftoken, content-type, accept',
        }
        return '', 200, headers

    # The javascript returns a value of toggle based on whether it's currently ready or picked up
    data = json.loads(request.data)
    toggle = data['toggle']
    order_num = data['order_num']

    cur = db.cursor()

    # Update the item to either ready or picked up
    if toggle:
        cur.execute("UPDATE order_data SET status='Ready' where order_num={}".format(str(order_num)))
        db.commit()
    else:
        cur.execute("UPDATE order_data SET status='Picked Up' where order_num={}".format(str(order_num)))
        db.commit()

    # Returns response code
    resp = {'status'  : 'Nice'}
    resp = json.dumps(resp)
    resp = Response(resp, status=200, mimetype='application/json')
    return resp


@login_manager.unauthorized_handler
def unauthorized_callback():
    """If login is required, redirect to the login page
    """
    return redirect('/login')


@app.route('/', methods=['GET','POST'])
@login_required
def home_page():
    """Pull all data from DB and render the main page

    This is the primary usage of our web application. This function pulls everything
    out of the data base and renders the home template. The home template takes all
    of the datbase data and builds a Bootstrap table with customer name, phone, order items,
    Cloudinary links to the items, a ready toggle butotn, the timestamp, and some notes.
    """
    cur = db.cursor()
    data = []

    # If we're receiving a post request that means that the 'Display All' button was clicked
    if request.method == "GET":
        cur.execute("SELECT * FROM order_data where status=\'Ready\'")
    else:
        cur.execute("SELECT * FROM order_data")

    # Loops through everything in order_data and fills in the data with a map for each order
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
            'timestamp': str(timestamp - timedelta(hours=5)),
            'num_items': num_items,
        })

        data[-1].setdefault('urls', [])
        data[-1].setdefault('items', [])
        data[-1].setdefault('signatures', [])

        # Builds all of the Cloudinary URLs for the items in the order
        for num in range(1,int(num_items)+1):
            data[-1]['urls'].append('http://res.cloudinary.com/du0tdfvpl/order_' + str(number) + '_' + str(num))

        # Builds all of the item information for the items in the order
        cur2 = db.cursor()
        cur2.execute('SELECT * FROM good_items WHERE url=' + number)
        db.commit()
        for row in cur2.fetchall():
            item, sig, num = row
            data[-1]['items'].append(item)
            data[-1]['signatures'].append(sig)

    # In a post request we set the toggle to True
    if request.method == 'POST':
        return render_template('home.html', data=data, toggle=True)

    return render_template('home.html', data=data, toggle=False)

@app.route('/login', methods=['GET', 'POST'])
def login():
    """Render the login page

    Login page is necessary to keep the application secure.
    Users should not be able to access any page without being authenticated
    in the users database.

    If this page is accessed with a GET request it will render the login page.
    If this page is accessed with a POST request it will attemt to login and
    forward the the homepage.
    """
    if request.method == 'POST':
        username = request.form['username']
        password = request.form['password']

        # Loop through the users in the database. If any match the credentials, sign in
        cur = db.cursor()
        cur.execute("SELECT * FROM good_users")
        db.commit()
        data = []
        for row in cur.fetchall():
            username_db, password_db = row

            # Password in database is hashed
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
    """Render the charts page

    When the charts page is requested this function gathers data from the DB
    and groups data accordingly for the charts to use.

    Currently the graphs include:
    A bar graph that displays top sold items all time.
    A line graph that displays all items sold in the past week.
    A pie graph that displays most popular days of the week.
    """

    cur = db.cursor()

    # Generate most popular item data
    # Loops over everything and keeps a list of the most popular labels. Returns the top 8
    cur.execute("SELECT item FROM good_items")
    db.commit()
    item_list = []
    for row in cur.fetchall():
        item_list.append(row[0].strip())
    item_list = sorted([(item_list.count(x),x) for x in set(item_list)], reverse=True)
    bar_values = [x[0] for x in item_list[:8]]
    bar_labels = [x[1] for x in item_list[:8]]

    # Generate days of week data and last 7 days data
    cur.execute("SELECT timestamp, num_items FROM order_data")
    db.commit()
    day_values = [0] * 7
    day_labels = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']

    # date_map keeps track of sales in the last 7 days
    date_map = {}
    for i in range(7,0,-1):
        date_map.setdefault((datetime.today() - timedelta(days=i)).strftime('%b %d'), 0)
    
    for row in cur.fetchall():
        timestamp, num_items = row
        # If the timestamp matches a day in the last 7 days, increment date map
        if timestamp.strftime('%b %d') in date_map:
            date_map[timestamp.strftime('%b %d')] += 1

        # Increment every day of the week
        day_values[day_labels.index(timestamp.strftime('%a'))] += 1

    max_weekly_val = 0
    week_values = []
    week_labels = sorted(list(date_map.keys()))

    # Find the max value in the last 7 days
    for label in week_labels:
        if date_map[label] > max_weekly_val:
            max_weekly_val = date_map[label]
        week_values.append(date_map[label])

    return render_template('charts.html', week_labels=week_labels, week_values=week_values, 
                            max_weekly_val=max_weekly_val, bar_labels=bar_labels, bar_values=bar_values,
                            day_labels=day_labels, day_values=day_values)


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
