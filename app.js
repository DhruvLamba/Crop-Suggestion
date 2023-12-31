const express = require("express");
const mongoose = require("mongoose");
const bodyParser = require("body-parser");
const path = require("path");

const app = express();
const port = 3000;

// Connect to MongoDB
mongoose.connect(
  "mongodb+srv://ayushsk0000:Ucsp7hehpIlapdsJ@upescsa.7bupqwk.mongodb.net/Dhruv",
  {
    useNewUrlParser: true,
    useUnifiedTopology: true,
  }
);

// Create a schema for the user model
const userSchema = new mongoose.Schema({
  username: String,
  password: String,
});

// Create a User model based on the schema
const User = mongoose.model("Dhruv", userSchema);

// Middleware for parsing JSON data
app.use(bodyParser.json());

// Serve static files (your HTML, CSS, and JS)
app.use(express.static("public"));
app.use(express.static(path.join(__dirname, 'public')));

// Register endpoint to handle user registration
app.post("/register", async (req, res) => {
  const { newUsername, newPassword } = req.body;

  try {
    // Check if the username is already taken
    const existingUser = await User.findOne({ username: newUsername });
    if (existingUser) {
      return res.status(400).json({ error: "Username already taken" });
    }

    // Create a new user in MongoDB
    const newUser = new User({
      username: newUsername,
      password: newPassword,
    });
    await newUser.save();

    // Respond with success message
    res.status(200).json({ message: "Registration successful!" });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: "Internal server error" });
  }
});

// Login endpoint to handle user login
app.post("/login", async (req, res) => {
  const { username, password } = req.body;

  try {
    // Check if the user exists
    const existingUser = await User.findOne({ username });

    if (!existingUser) {
      return res.status(401).json({ error: "User not found" });
    }

    // Check if the password is correct
    if (existingUser.password !== password) {
      return res.status(401).json({ error: "Incorrect password" });
    }

    // Respond with success message or additional user data
    res.status(200).json({ message: "Login successful!" });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: "Internal server error" });
  }
});

// Route for the root path
app.get("/", (req, res) => {
  res.sendFile(path.join(__dirname, "Register1.html"));
});

app.get("/index.html", (req, res) => {
  res.sendFile(path.join(__dirname, "index.html"));
});

app.get("/Register1.html", (req, res) => {
  res.sendFile(path.join(__dirname, "Register1.html"));
});
// Replace the suggestCrop function in your existing app.js file
function suggestCrop() {
    const soilType = document.getElementById("soilType").value;
    const phValue = document.getElementById("phValue").value;
    const temperature = document.getElementById("temperature").value;
    const salinity = document.getElementById("salinity").value;
    const cropType = document.getElementById("cropType").value;

    // Make a Fetch API request to your Java backend
    fetch(`http://localhost:4567/suggestCrop?soilType=${soilType}&phValue=${phValue}&temperature=${temperature}&salinity=${salinity}&cropType=${cropType}`)
        .then(response => response.json())
        .then(data => {
            // Process the response data and update the UI
            const cropResult = data.length > 0
                ? `Based on the input values, the suggested crops are: ${data.join(", ")}`
                : "No crops found for the given conditions.";

            document.getElementById("cropResult").innerText = cropResult;
        })
        .catch(error => {
            console.error(error);
            alert("An error occurred. Please try again.");
        });
}

// Start the server
app.listen(port, () => {
  console.log(`Server is running on http://localhost:${port}`);
});
