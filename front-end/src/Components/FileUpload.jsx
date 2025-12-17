import React from "react";
import {
  Button,
  Card,
  CardContent,
  CardActions,
  Typography,
  Box,
  Chip,
} from "@mui/material";
import CloudUploadIcon from "@mui/icons-material/CloudUpload";

export const FileUpload = ({ onUpload, onSubmit, uploading }) => {
  const fileInputRef = React.useRef(null);
  const [localFileName, setLocalFileName] = React.useState("");

  const handleFileChange = (e) => {
    const file = e.target.files?.[0];
    if (file) {
      setLocalFileName(file.name);
      if (onUpload) {
        onUpload(file);
      }
    }
  };

  const openFilePicker = () => {
    if (fileInputRef.current) {
      fileInputRef.current.click();
    }
  };

  const handleClickGenerate = () => {
    if (onSubmit) {
      onSubmit();
    }
  };

  return (
    <Box
      sx={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        p: 3,
      }}
    >
      <Card
        elevation={6}
        sx={{
          maxWidth: 640,
          width: "100%",
          borderRadius: 3,
          overflow: "hidden",
        }}
      >
        <Box
          sx={{
            background: "linear-gradient(135deg, #1e88e5, #3949ab)",
            color: "#fff",
            p: 3,
          }}
        >
          <Typography variant="h5" fontWeight={600}>
            Meetwise – Upload Transcript
          </Typography>
          <Typography variant="body2" sx={{ opacity: 0.9, mt: 1 }}>
            Upload a meeting transcript text file and Meetwise will generate
            Jira-ready action items and a concise summary.
          </Typography>
        </Box>

        <CardContent sx={{ p: 4 }}>
          <input
            type="file"
            accept=".txt,.md,.log"
            ref={fileInputRef}
            onChange={handleFileChange}
            style={{ display: "none" }}
          />

          <Box
            sx={{
              border: "2px dashed #bbdefb",
              borderRadius: 2,
              p: 4,
              textAlign: "center",
              backgroundColor: "#f5f7fb",
            }}
          >
            <CloudUploadIcon sx={{ fontSize: 48, color: "#1e88e5", mb: 1 }} />
            <Typography variant="subtitle1" fontWeight={600}>
              Drop your transcript here
            </Typography>
            <Typography variant="body2" sx={{ color: "text.secondary", mt: 0.5 }}>
              or click the button below to browse files
            </Typography>

            <Button
              variant="contained"
              sx={{ mt: 2, borderRadius: 999 }}
              startIcon={<CloudUploadIcon />}
              onClick={openFilePicker}
            >
              Choose File
            </Button>

            {localFileName && (
              <Box sx={{ mt: 2 }}>
                <Chip
                  label={localFileName}
                  color="primary"
                  variant="outlined"
                  size="small"
                />
              </Box>
            )}
          </Box>
        </CardContent>

        <CardActions sx={{ p: 3, pt: 0, justifyContent: "flex-end" }}>
          <Button
            variant="contained"
            color="primary"
            onClick={handleClickGenerate}
            disabled={uploading}
            sx={{ borderRadius: 999, px: 4 }}
          >
            {uploading ? "Processing…" : "Generate Summary"}
          </Button>
        </CardActions>
      </Card>
    </Box>
  );
};
