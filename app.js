const express = require("express");
const mongoose = require("mongoose");
const bodyParser = require("body-parser");
const path = require("path");

const app = express();
const port = 3000;

mongoose.connect(
  "mongodb+srv://ayushsk0000:Ucsp7hehpIlapdsJ@upescsa.7bupqwk.mongodb.net/Dhruv",
  {
    useNewUrlParser: true,
    useUnifiedTopology: true,
  }
);


const userSchema = new mongoose.Schema({
  username: String,
  password: String,
});


const User = mongoose.model("Dhruv", userSchema);

app.use(bodyParser.json());


app.use(express.static("public"));
app.use(express.static(path.join(__dirname, 'public')));

app.post("/register", async (req, res) => {
  const { newUsername, newPassword } = req.body;

  try {

    const existingUser = await User.findOne({ username: newUsername });
    if (existingUser) {
      return res.status(400).json({ error: "Username already taken" });
    }


    const newUser = new User({
      username: newUsername,
      password: newPassword,
    });
    await newUser.save();


    res.status(200).json({ message: "Registration successful!" });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: "Internal server error" });
  }
});

app.post("/login", async (req, res) => {
  const { username, password } = req.body;

  try {

    const existingUser = await User.findOne({ username });

    if (!existingUser) {
      return res.status(401).json({ error: "User not found" });
    }

    if (existingUser.password !== password) {
      return res.status(401).json({ error: "Incorrect password" });
    }


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

function suggestCrop() {
    const soilType = document.getElementById("soilType").value;
    const phValue = document.getElementById("phValue").value;
    const temperature = document.getElementById("temperature").value;
    const salinity = document.getElementById("salinity").value;
    const cropType = document.getElementById("cropType").value;

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


app.listen(port, () => {
  console.log(`Server is running on http://localhost:${port}`);
});
