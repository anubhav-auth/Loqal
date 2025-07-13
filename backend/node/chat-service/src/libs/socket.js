import { Server } from "socket.io"
import http, { createServer } from "http"
import express from "express"

const app = express();
const server = http.createServer(app);

export { app, server };