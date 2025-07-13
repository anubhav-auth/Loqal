import express from "express"
import dotenv from "dotenv"
dotenv.config();
import { connectDB } from "./src/libs/db.js";
import {app,server} from "./src/libs/socket.js"
const port = 3000;
app.use(express.json());

server.listen(port, () => {
    console.log("server is on port: " + port);
    connectDB();
})
