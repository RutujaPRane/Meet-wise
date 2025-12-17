import React, { useState } from "react";
import { FileUpload } from "./FileUpload";
import { CombinedView } from "./CombinedView";

export const App = () => {
  const [selectedFile, setSelectedFile] = useState(null);
  const [fileId, setFileId] = useState(null);
  const [uploading, setUploading] = useState(false);

  // called when user picks a file in FileUpload
  const handleUpload = (file) => {
    console.log("📁 Selected file:", file);
    setSelectedFile(file);
  };

  // called when user clicks "Generate Summary"
  const handleGenerateSummary = async () => {
    if (!selectedFile) {
      alert("Please select a transcript file first.");
      return;
    }

    try {
      setUploading(true);

      const formData = new FormData();
      // IMPORTANT: the name "file" must match what your Spring controller expects
      formData.append("file", selectedFile);

      console.log("🚀 Uploading to backend…");
      const resp = await fetch(
        "http://localhost:9000/smartmeet/generate-summary",
        {
          method: "POST",
          body: formData,
        }
      );

      if (!resp.ok) {
        const text = await resp.text();
        console.error("Upload failed:", text);
        alert("Upload failed. Check backend logs.");
        return;
      }

      const data = await resp.json();
      console.log("✅ Backend response:", data);

      // backend sends { "fileID": "xxxx" } (from your screenshot)
      const id = data.fileID || data.fileId;
      setFileId(id);
    } catch (e) {
      console.error("Upload error", e);
      alert("Upload error. See console.");
    } finally {
      setUploading(false);
    }
  };

  // Before upload => show upload screen
  if (!fileId) {
    return (
      <FileUpload
        onUpload={handleUpload}
        onSubmit={handleGenerateSummary}
        uploading={uploading}
      />
    );
  }

  // After upload => show dashboard
  return <CombinedView />;
};

export default App;
