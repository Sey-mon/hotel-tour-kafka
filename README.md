# Hotel Tour Kafka

A distributed hotel and tour booking system built with Apache Kafka, JavaFX, and SQLite. This project demonstrates event-driven architecture with CRUD operations, real-time data synchronization, and microservices communication.

## 🏗️ Architecture

This project consists of three main modules:

1. **shared** - Common event models and JSON utilities
2. **gui-producer** - JavaFX GUI application for managing hotels, tours, and bookings
3. **booking-consumer** - Kafka consumer that persists events to SQLite database

```
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│  GUI Producer   │ ───────>│  Apache Kafka   │ ───────>│  Consumer App   │
│   (JavaFX)      │  Events │   (Broker)      │  Events │  (SQLite DB)    │
└─────────────────┘         └─────────────────┘         └─────────────────┘
        ↑                                                          │
        └──────────────────── Reads from DB ──────────────────────┘
```

## 📋 Features

### Hotel Management
- ✅ Create new hotels with name, city, and price per night
- ✅ Update existing hotel information
- ✅ Delete hotels
- ✅ Live auto-refresh (updates every 2 seconds)
- ✅ View all hotels in a table

### Tour Management (Coming Soon)
- Create tours with name, city, and price
- Update and delete tours
- Link tours to specific cities

### Booking Management (Coming Soon)
- Create bookings with customer name, hotel, tour, and nights
- Link bookings to hotels and tours
- View booking history

### Technical Features
- **Event-Driven Architecture**: All operations publish events to Kafka
- **Real-time Synchronization**: Consumer processes events and updates database
- **Auto-refresh UI**: GUI automatically refreshes data every 2 seconds
- **CRUD Operations**: Full Create, Read, Update, Delete functionality
- **Error Handling**: Comprehensive error logging and user feedback
- **Database Persistence**: SQLite for lightweight data storage

## 🚀 Prerequisites

- **Java 17+** (Project uses Java language features like records and text blocks)
- **Apache Kafka** (Running locally on `localhost:9092`)
- **Gradle** (Included via wrapper)
- **JavaFX 21** (Automatically downloaded by Gradle)

## 📦 Project Structure

```
hotel-tour-kafka/
├── shared/
│   └── src/main/java/com/app/shared/
│       ├── events/           # Event classes (HotelCreatedEvent, etc.)
│       └── json/             # JSON serialization utilities
├── gui-producer/
│   └── src/main/java/com/app/gui/
│       ├── hotel/            # Hotel tab UI and operations
│       ├── MainApp.java      # JavaFX main application
│       └── ProducerCliApp.java  # CLI for testing
└── booking-consumer/
    └── src/main/java/com/app/consumer/
        ├── db/               # Database setup and queries
        └── BookingConsumerApp.java  # Kafka consumer
```

## 🔧 Setup Instructions

### 1. Start Apache Kafka

First, start Zookeeper:
```bash
bin\windows\zookeeper-server-start.bat config\zookeeper.properties
```

Then start Kafka broker:
```bash
bin\windows\kafka-server-start.bat config\server.properties
```

### 2. Build the Project

```bash
.\gradlew build -x test
```

### 3. Run the Consumer (Terminal 1)

```bash
.\gradlew :booking-consumer:run
```

You should see:
```
Consumer started. Listening...
```

### 4. Run the GUI Producer (Terminal 2)

```bash
.\gradlew :gui-producer:run
```

The JavaFX application window will open.

## 📖 How to Use

### Adding a Hotel

1. Fill in the form fields:
   - **Name**: Hotel name (e.g., "Grand Plaza Hotel")
   - **City**: City location (e.g., "Manila")
   - **Price**: Price per night (e.g., "2500")
2. Click **Add** button
3. The hotel will be sent to Kafka, processed by the consumer, and appear in the table within 2 seconds

### Updating a Hotel

1. Click on a hotel row in the table
2. The form fields will populate with the hotel's data
3. Modify the fields as needed
4. Click **Update** button
5. Changes will be synced across the system

### Deleting a Hotel

1. Click on a hotel row in the table
2. Click **Delete** button
3. Confirm the deletion in the dialog
4. The hotel will be removed from the database

### Manual Refresh

Click the **Refresh** button to immediately reload data from the database (though auto-refresh happens every 2 seconds).

## 🎯 Kafka Topics

The application uses the following Kafka topics:

- `hotel.created` - New hotel created
- `hotel.updated` - Hotel information updated
- `hotel.deleted` - Hotel removed
- `tour.created` - New tour created
- `tour.updated` - Tour information updated
- `tour.deleted` - Tour removed
- `booking.created` - New booking made
- `booking.deleted` - Booking cancelled

## 🗄️ Database Schema

The SQLite database (`booking.db`) is created in the `booking-consumer/` directory with the following tables:

### hotels
```sql
CREATE TABLE hotels (
    hotel_id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    city TEXT NOT NULL,
    price_per_night REAL NOT NULL
)
```

### tours
```sql
CREATE TABLE tours (
    tour_id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    city TEXT NOT NULL,
    price REAL NOT NULL
)
```

### bookings
```sql
CREATE TABLE bookings (
    booking_id TEXT PRIMARY KEY,
    customer_name TEXT NOT NULL,
    hotel_id TEXT NOT NULL,
    tour_id TEXT,
    nights INTEGER NOT NULL,
    created_at TEXT NOT NULL
)
```

## 🔍 Event Flow Example

When you add a hotel:

1. **GUI** validates input and creates `HotelCreatedEvent`
2. **KafkaProducer** sends event to `hotel.created` topic
3. **Kafka Broker** stores the event
4. **Consumer** polls Kafka and receives the event
5. **Consumer** deserializes JSON to `HotelCreatedEvent` object
6. **Consumer** executes `INSERT OR REPLACE` into SQLite
7. **GUI** auto-refreshes after 2 seconds and displays the new hotel

## 🐛 Debugging

The application includes extensive debug logging:

```
[DEBUG] onAdd() called
[DEBUG] Creating hotel: Grand Plaza, Manila, 2500.0
[DEBUG] Hotel created successfully: a1b2c3d4-...
[DEBUG] Refreshing hotel list...
[DEBUG] Loaded 6 hotel(s)
```

Check console output for:
- Button click events
- Kafka connection info
- Database operations
- Error stack traces

## 🛠️ Technologies Used

- **Java 17** - Modern Java features (records, text blocks, switch expressions)
- **Apache Kafka 3.9.1** - Distributed event streaming
- **JavaFX 21** - Desktop GUI framework
- **SQLite 3.45.3** - Embedded database
- **Jackson 2.17.2** - JSON serialization/deserialization
- **Gradle 9.2** - Build automation

## 📝 Configuration

### Kafka Bootstrap Server

Default: `localhost:9092`

Change in:
- `MainApp.java` (line 15)
- `BookingConsumerApp.java` (line 26)

### Auto-refresh Interval

Default: 2000ms (2 seconds)

Change in `HotelsTab.java`:
```java
private static final int AUTO_REFRESH_INTERVAL_MS = 2000;
```

### Consumer Delay

Default: 600ms delay after CRUD operations

Change in `HotelsTab.java`:
```java
private static final int REFRESH_DELAY_MS = 600;
```

## 🚧 Known Issues

- Consumer must be running before performing CRUD operations
- Database path resolution assumes specific directory structure
- No authentication/authorization implemented
- Single consumer group (no horizontal scaling yet)

## 🔮 Future Enhancements

- [ ] Add Tours tab with full CRUD operations
- [ ] Add Bookings tab for managing reservations
- [ ] Implement search and filtering
- [ ] Add data validation and constraints
- [ ] Implement pagination for large datasets
- [ ] Add export to CSV/PDF functionality
- [ ] Implement Kafka Streams for analytics
- [ ] Add Docker support for easy deployment
- [ ] Implement authentication and user roles

## 📄 License

This project is open source and available for educational purposes.

## 👤 Author

Created as a demonstration of event-driven architecture with Kafka and JavaFX.

## 🤝 Contributing

Feel free to fork this project and submit pull requests with improvements!

---

**Note**: Make sure Apache Kafka is running before starting the application, otherwise the producer will fail to connect.
