import React, { useEffect, useState } from "react";
import { DataGrid } from "@mui/x-data-grid";
import Box from "@mui/material/Box";

const columns = [
  { field: "summary", headerName: "Summary", width: 180, editable: true },
  { field: "description", headerName: "Description", width: 220, editable: true },
  { field: "assignee", headerName: "Assignee", width: 140, editable: true },
  {
    field: "priority",
    headerName: "Priority",
    width: 120,
    editable: true,
    type: "singleSelect",
    valueOptions: ["Highest", "High", "Medium", "Low", "Lowest"],
  },
  {
    field: "jiraIssueUrl",
    headerName: "Jira",
    width: 220,
    renderCell: (params) => {
      const val = params.value;
      if (!val) {
        return <span style={{ color: "#9e9e9e" }}>Not created</span>;
      }
      if (typeof val === "string" && val.startsWith("http")) {
        return (
          <a
            href={val}
            target="_blank"
            rel="noopener noreferrer"
            style={{ fontSize: "0.85rem" }}
          >
            {val.split("/").pop()}
          </a>
        );
      }
      return <span style={{ color: "#c62828" }}>{val}</span>;
    },
  },
  {
    field: "issueType",
    headerName: "Issue Type",
    width: 130,
    editable: true,
    type: "singleSelect",
    valueOptions: ["Epic", "Task", "Subtask", "Bug", "Story"],
  },
  { field: "id", headerName: "id", width: 150 },
  { field: "fileId", headerName: "file id", width: 150 },
];

const columnVisibilityModel = {
  id: false,
  fileId: false,
};

export const extractRows = (eventData) => {
  const extractedRows = [];
  eventData.forEach((e) => {
    let count = 0;
    e.actionItems.forEach((f) => {
      extractedRows.push({
        fileId: e.fileId,
        id: e.id,
        rowId: `${e.fileId}-${e.id}-${count}`,
        ...f,
      });
      count += 1;
    });
  });
  return extractedRows;
};

export const extractRowsFromSubmitResponse = (submitChunks, fileId) => {
  const extractedRows = [];
  submitChunks.forEach((chunk) => {
    let count = 0;
    chunk.chunkItems.forEach((item) => {
      extractedRows.push({
        fileId,
        id: chunk.chunkId,
        rowId: `${fileId}-${chunk.chunkId}-${count}`,
        ...item,
      });
      count += 1;
    });
  });
  return extractedRows;
};

export const ActionItemList = ({ apiRef, resetRows }) => {
  const [dataRows, setDataRows] = useState([]);
  const [loading, setLoading] = useState(true);

  // SSE for incoming items
  useEffect(() => {
    const eventSource = new EventSource("http://localhost:9000/smartmeet/subscribe");

    eventSource.onmessage = (event) => {
      console.log("RAW SSE event.data:", event.data);
      let jsonData;
      try {
        jsonData = JSON.parse(event.data);
      } catch (e) {
        console.error("Failed to parse SSE data", event.data, e);
        return;
      }
      console.log("received", jsonData.fileId, jsonData.actionItems);

      setDataRows((prevData) => {
        const currentData = extractRows([jsonData]);
        return [...prevData, ...currentData];
      });
    };

    eventSource.onerror = (error) => {
      console.error("EventSource failed:", error);
    };

    return () => {
      eventSource.close();
    };
  }, []);

  // When parent resets rows after /submit
  useEffect(() => {
    if (Array.isArray(resetRows) && resetRows.length > 0) {
      console.log("Resetting rows from parent:", resetRows);
      setDataRows(resetRows);
    }
  }, [resetRows]);

  useEffect(() => {
    setLoading(dataRows.length === 0);
  }, [dataRows]);

  return (
    <Box sx={{ height: 420, width: "100%" }}>
      <DataGrid
        apiRef={apiRef}
        rows={dataRows}
        columns={columns}
        getRowId={(r) => r.rowId}
        disableColumnSelector
        columnVisibilityModel={columnVisibilityModel}
        loading={loading}
        sx={{
          border: "none",
          "& .MuiDataGrid-columnHeaders": {
            backgroundColor: "#e8eaf6",
            color: "#1a237e",
            fontWeight: 600,
            borderBottom: "1px solid #c5cae9",
          },
          "& .MuiDataGrid-row:nth-of-type(even)": {
            backgroundColor: "#fafbff",
          },
          "& .MuiDataGrid-cell": {
            borderBottom: "1px solid #eef1f7",
          },
          "& .MuiDataGrid-row:hover": {
            backgroundColor: "#e3f2fd",
          },
        }}
        slotProps={{
          loadingOverlay: {
            variant: "linear-progress",
            noRowsVariant: "linear-progress",
          },
        }}
      />
    </Box>
  );
};
