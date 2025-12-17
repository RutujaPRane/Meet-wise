import React, { useState } from "react";
import { Container, Grid, Paper, Typography, Box, Button } from "@mui/material";
import { ActionItemList, extractRowsFromSubmitResponse } from "./ActionItemList";
import { SummaryBox } from "./Summary";
import { useGridApiRef } from "@mui/x-data-grid";

export const CombinedView = () => {
  const [resetRows, setResetRows] = useState([]);
  const [summaryData, setSummaryData] = useState("");
  const apiRef = useGridApiRef();

  const handleSummaryDataChange = (newData) => {
    setSummaryData(newData);
  };

  const handleSubmit = () => {
    let fileId = "";
    const chunksRequest = {};

    apiRef.current.getSortedRows().forEach((d) => {
      fileId = d.fileId;

      if (!chunksRequest[d.id]) {
        chunksRequest[d.id] = [];
      }

      chunksRequest[d.id].push({
        issueType: d.issueType,
        priority: d.priority,
        summary: d.summary,
        description: d.description,
        assignee: d.assignee,
        jiraIssueUrl: d.jiraIssueUrl || "",
      });
    });

    const requestBody = {
      fileId,
      summary: summaryData,
      chunkTOs: [],
    };

    Object.keys(chunksRequest).forEach((chunkId) => {
      requestBody.chunkTOs.push({
        chunkId,
        chunkItems: chunksRequest[chunkId],
      });
    });

    console.log("Submit request body:", requestBody);

    fetch(`http://localhost:9000/smartmeet/submit/${fileId}`, {
      method: "POST",
      body: JSON.stringify(requestBody),
      headers: {
        "Content-Type": "application/json",
      },
    })
      .then((r) => r.json())
      .then((resp) => {
        console.log("Submit json response", resp);
        const newRows = extractRowsFromSubmitResponse(resp.actionItems || [], resp.fileId);
        setResetRows(newRows);
      })
      .catch((err) => console.error("Submit failed", err));
  };

  return (
    <Box sx={{ minHeight: "100vh", py: 4 }}>
      <Container maxWidth="lg">
        {/* Header */}
        <Box sx={{ mb: 3 }}>
          <Typography variant="h4" fontWeight={600} sx={{ color: "#1a237e" }}>
            Meetwise Dashboard
          </Typography>
          <Typography variant="body2" sx={{ color: "text.secondary", mt: 0.5 }}>
            Review AI-generated action items, tweak details, and send them to Jira along with a polished meeting summary.
          </Typography>
        </Box>

        <Grid container spacing={3}>
          {/* Action Items */}
          <Grid item xs={12} md={7}>
            <Paper
              elevation={4}
              sx={{
                borderRadius: 3,
                overflow: "hidden",
                display: "flex",
                flexDirection: "column",
                height: "100%",
              }}
            >
              <Box
                sx={{
                  px: 3,
                  py: 2,
                  borderBottom: "1px solid #e3e5f0",
                  background: "linear-gradient(135deg,#1e88e5,#3949ab)",
                  color: "#fff",
                }}
              >
                <Typography variant="h6" fontWeight={600}>
                  Action Items
                </Typography>
                <Typography variant="body2" sx={{ opacity: 0.85 }}>
                  Edit or refine items before creating Jira tickets.
                </Typography>
              </Box>

              <Box sx={{ p: 2, flex: 1 }}>
                <ActionItemList apiRef={apiRef} resetRows={resetRows} />
              </Box>
            </Paper>
          </Grid>

          {/* Summary + Submit */}
          <Grid item xs={12} md={5}>
            <Paper
              elevation={4}
              sx={{
                borderRadius: 3,
                overflow: "hidden",
                display: "flex",
                flexDirection: "column",
                height: "100%",
              }}
            >
              <SummaryBox data={summaryData} setData={handleSummaryDataChange} />

              <Box
                sx={{
                  px: 3,
                  py: 2,
                  borderTop: "1px solid #e3e5f0",
                  display: "flex",
                  justifyContent: "flex-end",
                  backgroundColor: "#fafafa",
                }}
              >
                <Button
                  variant="contained"
                  color="primary"
                  onClick={handleSubmit}
                  sx={{ borderRadius: 999, px: 4 }}
                >
                  Submit to Jira
                </Button>
              </Box>
            </Paper>
          </Grid>
        </Grid>
      </Container>
    </Box>
  );
};
