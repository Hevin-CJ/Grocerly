# Grocerly — Customer Application

Grocerly is a native Android customer application for grocery shopping, featuring real-time product browsing, coupon redemption, secure payment processing, offline-tolerant order placement, and an AI-powered shopping assistant.

---

## 🚀 Key Features

* **Secure Authentication**: Multi-channel login using **Firebase Authentication** (Google ID/Credentials Manager, Facebook SDK, and email/password).
* **Browsing & Search**: Search products across categories, toggle favorites, and maintain wishlists.
* **Offline-Resilient Cart**: Interactive shopping cart powered by a local **Room Database** cache that automatically synchronizes with Cloud Firestore when online.
* **Geolocated Deliveries**: Built-in address selection using **Google Maps SDK** and **Google Play Services Location APIs**.
* **Offline-Tolerant Checkout**: Uses **WorkManager** (`PlaceOrderWorker`) to queue pending orders in a local Room database, ensuring 100% successful order routing to Firestore during network instability.
* **Integrated Payments**: Fully integrated **Razorpay SDK** checkout for secure payment processing.
* **Real-time Order Tracking**: Track delivery milestones (`PENDING` ➔ `ACCEPTED` ➔ `READY` ➔ `SHIPPED` ➔ `DELIVERED`) with a visual step-progress indicator, driven by Firestore snapshot listeners.
* **AI Support Agent**: Embedded chatbot assistant powered by **Gemini 2.5 Flash** (via **Firebase AI SDK**) that answers culinary questions, creates shopping lists, and provides step-by-step app navigation.
* **FCM Push Notifications**: Receives real-time status notifications for order updates in the background.

---

## 🛠️ Architecture & Tech Stack

* **Language**: Kotlin
* **Architecture**: MVVM (Model-View-ViewModel)
* **UI Framework**: XML Layouts + ViewBinding & DataBinding (Single-Activity pattern with Jetpack Navigation Component and Safe Args)
* **Dependency Injection**: Dagger Hilt
* **Local Caching**: Room DB & Preferences DataStore
* **Web Client**: Retrofit & Moshi Converter
* **Image Loading**: Coil & Glide
* **Background Processing**: WorkManager

---

## 📂 Project Structure

```
app/src/main/java/com/example/grocerly/
│
├── activity/          # MainActivity (Single activity controller)
├── fragments/         # UI Screens (Home, Cart, Profile, Checkout, Chat, etc.)
├── adapters/          # RecyclerView Adapters for lists
├── viewmodel/         # ViewModels managing UI states and business logic
├── Repository/        # Remote and Local repositories (clean data layer abstraction)
├── room/              # Room local database entities, DAOs, and converters
├── preferences/       # Preferences DataStore helper scripts
├── worker/            # Background tasks (PlaceOrderWorker, CouponWorker)
├── di/                # Hilt modules (Firebase, Network, Room, GenAI)
├── model/             # Data models (Product, Order, Account, Address, etc.)
└── utils/             # Helper utils (FCM service, custom extensions)
```

---

## 🔄 Core Database Schema (Firestore)

Grocerly utilizes a real-time **Cloud Firestore** structure:
* `accounts/{userId}`: Stores user credentials and profile details.
* `users/{userId}/cart`: Real-time cart items collection.
* `users/{userId}/address`: Geolocation delivery addresses.
* `users/{userId}/orders/{orderId}`: Historical customer orders.
* `users/{userId}/earned_coupons`: Active partner-specific promo codes available for checkout.

---

## ⚙️ Getting Started

### 📋 Prerequisites
1. Android Studio Ladybug (or newer)
2. JDK 11+
3. A Firebase project with Firestore, Authentication, Cloud Storage, and FCM enabled.

### 🔑 Configuration Setup
1. Download `google-services.json` from your Firebase console and place it in the `app/` folder.
2. In your `local.properties` file, configure the following API keys:
   ```properties
   GOOGLE_SERVER_CLIENT_ID=your_google_client_id
   ```
3. Set your Razorpay API key in the `AndroidManifest.xml` or via project string resources.

### 🏃 Running the Project
1. Clone this repository:
   ```bash
   git clone https://github.com/Hevin-CJ/Grocerly.git
   ```
2. Open the project in Android Studio.
3. Sync Gradle and run the project on an emulator or active device (target SDK 35).
