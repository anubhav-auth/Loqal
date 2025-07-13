import express from "express"
import dotenv from "dotenv"
dotenv.config();
import { connectDB } from "./src/libs/db.js";
const app = express();
const port = 3000;
app.use(express.json());

app.listen(port, () => {
    console.log("server is on port: " + port);
    connectDB();
})
