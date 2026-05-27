# 💳 Apna Wallet (FinFurcate)
> A modern, intelligent personal finance tracker and virtual envelope budgeting application built natively for Android.

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?&style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Material Design 3](https://img.shields.io/badge/Material_Design_3-757575?style=for-the-badge&logo=materialdesign&logoColor=white)

## ✨ Overview
Apna Wallet is designed to help users take control of their finances through the "Virtual Envelope" budgeting method. Unlike traditional trackers that require manual entry, Apna Wallet utilizes background services to autonomously parse transaction notifications, providing a real-time, dynamic dashboard of expenditures, savings, and active streaks. 

Developed as a Mini-Project submission, focusing on modern Android architecture, state management, and custom UI/UX design.

---

## 🚀 Key Features

* **✉️ Virtual Envelopes:** Create custom-colored envelopes to allocate budgets for specific categories (e.g., Rent, Groceries, Celebrations).
* **🤖 FinBot Assistant:** An integrated chatbot to guide users through the app's features and financial tracking methods.
* **📊 Dynamic Analytics:** Custom-built data visualization featuring real-time Donut Charts and Bar Graphs to track spending vs. saving over time.
* **🔄 Smart Categorizer:** Automatically detects transaction types (e.g., "Veg", "Recharge") and assigns appropriate UI icons and color themes.
* **💸 Frictionless Transfers:** Move funds between virtual envelopes without registering fake transactions, maintaining accurate "Total Spent" data.
* **📱 QR Receiving:** Generate custom UPI QR codes tied directly to specific envelopes for seamless receiving.
* **🌙 Adaptive UI:** Fully functional Light and Dark modes utilizing Jetpack Compose Material 3 standards.

---

## 🛠️ Tech Stack & Architecture

### **Frontend**
* **Language:** Kotlin
* **UI Toolkit:** Jetpack Compose (Declarative UI)
* **Navigation:** Compose Navigation / Scaffold Bottom Bar
* **Graphics:** Custom Canvas API for dynamic charts (No third-party charting libraries used)

### **Core Android Components**
* **Background Processing:** `NotificationListenerService` for secure, real-time transaction extraction.
* **State Management:** Hoisted `remember` states and `LaunchedEffect` for reactive UI updates.
* **Local Storage:** `SharedPreferences` utilized for lightweight, persistent session and financial data storage.
* **Lifecycle Awareness:** `LifecycleEventObserver` for seamless data refreshing on app resume.

---

## 📸 Screenshots
<img width="309" height="688" alt="image" src="https://github.com/user-attachments/assets/8f0f524e-7749-4447-92bf-bc08cf2d676c" />


## 🧠 Technical Highlights 
1. **State Hoisting:** Implemented advanced state hoisting to allow multiple screens (Dashboard, Wallets, Profile) to share and manipulate the same data instances simultaneously.
2. **Regex Parsing:** Built resilient logic to extract exact financial integers from standard banking push notifications, bypassing cluttered text.
3. **Canvas Drawing:** Built the analytics dashboard entirely from scratch using mathematical geometry (`drawArc`, `drawLine`) to calculate exact sweep angles based on real user data percentages.

---

<div align="center">
  <h3>Developed with ❤️ by Tasneem</h3>
  <p>B.Tech CSE Mini-Project</p>
</div>
