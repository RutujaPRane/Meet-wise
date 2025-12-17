import Box from '@mui/material/Box';
import TextField from '@mui/material/TextField';
import React, { useEffect, useState } from "react";
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import AppBar from '@mui/material/AppBar';
import LinearProgress from '@mui/material/LinearProgress';

export const SummaryBox = ({ data, setData }) => {
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    console.log("📡 Opening SSE connection for summary...");
    const eventSource = new EventSource("http://localhost:9000/smartmeet/subscribe-summary");

    eventSource.onmessage = (event) => {
      console.log("📝 SSE summary event.data:", event.data);
      setData(event.data);
    };

    eventSource.onerror = (error) => {
      console.error("❌ EventSource summary failed:", error);
    };

    return () => {
      console.log("🔌 Closing SSE summary connection");
      eventSource.close();
    };
  }, [setData]);

  useEffect(() => {
    if (data !== undefined && data !== null && data !== "") {
      setLoading(false);
    }
  }, [data]);

  return (
    <Box component="section" sx={{ flex: 1, display: "flex", flexDirection: "column" }}>
      <AppBar
        position="static"
        elevation={0}
        sx={{
          background: "linear-gradient(135deg,#1e88e5,#3949ab)",
        }}
      >
        <Toolbar>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1, textAlign: "center" }}>
            Summary
          </Typography>
        </Toolbar>
      </AppBar>

      {loading && <LinearProgress />}

      <Box sx={{ p: 2, flex: 1 }}>
        <TextField
          id="outlined-multiline-static"
          multiline
          fullWidth
          rows={7}
          placeholder="Summary Text"
          value={data ?? ""}
          onChange={(e) => setData(e.target.value)}
        />
      </Box>
    </Box>
  );
};
