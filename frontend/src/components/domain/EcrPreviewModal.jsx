import React from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Box,
  Typography,
  IconButton,
  Tooltip,
} from '@mui/material';
import CloseOutlinedIcon from '@mui/icons-material/CloseOutlined';
import DownloadOutlinedIcon from '@mui/icons-material/DownloadOutlined';
import ContentCopyOutlinedIcon from '@mui/icons-material/ContentCopyOutlined';
import VetanButton from '../ui/VetanButton';

export default function EcrPreviewModal({
  open = false,
  onClose,
  ecrContent = '',
  monthYear = '2026-11',
}) {
  const handleDownload = () => {
    const blob = new Blob([ecrContent], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `EPFO_ECR_${monthYear}.txt`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handleCopy = () => {
    navigator.clipboard?.writeText(ecrContent);
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="md"
      fullWidth
      PaperProps={{
        sx: {
          backgroundColor: '#0F172A',
          border: '1px solid rgba(255, 255, 255, 0.1)',
          borderRadius: '10px',
        },
      }}
    >
      <DialogTitle
        sx={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          pb: 1.5,
          borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
        }}
      >
        <Box>
          <Typography variant="h6" sx={{ fontWeight: 600 }}>
            Official EPFO ECR 2.0 Text File
          </Typography>
          <Typography variant="caption" sx={{ color: 'text.secondary' }}>
            25-column #~# delimiter format ready for upload on EPFO Unified Portal
          </Typography>
        </Box>
        <Tooltip title="Close modal">
          <IconButton aria-label="Close ECR preview modal" size="small" onClick={onClose} sx={{ color: 'text.secondary' }}>
            <CloseOutlinedIcon sx={{ fontSize: 18 }} />
          </IconButton>
        </Tooltip>
      </DialogTitle>

      <DialogContent sx={{ py: 2.5 }}>
        <Box
          sx={{
            p: 2,
            backgroundColor: '#090D16',
            borderRadius: '6px',
            border: '1px solid rgba(255, 255, 255, 0.08)',
            fontFamily: 'monospace',
            fontSize: '0.8rem',
            color: '#34D399',
            maxHeight: 380,
            overflowY: 'auto',
            whiteSpace: 'pre-wrap',
            wordBreak: 'break-all',
          }}
        >
          {ecrContent || 'No ECR content generated yet. Initiate and calculate a payroll run first.'}
        </Box>
      </DialogContent>

      <DialogActions
        sx={{
          px: 3,
          py: 2,
          borderTop: '1px solid rgba(255, 255, 255, 0.08)',
          display: 'flex',
          justifyContent: 'space-between',
        }}
      >
        <Tooltip title="Copy text">
          <IconButton aria-label="Copy ECR contents" size="small" onClick={handleCopy} sx={{ color: 'text.secondary' }}>
            <ContentCopyOutlinedIcon sx={{ fontSize: 18 }} />
          </IconButton>
        </Tooltip>
        <Box sx={{ display: 'flex', gap: 1.5 }}>
          <VetanButton variant="outlined" onClick={onClose}>
            Close
          </VetanButton>
          <VetanButton
            variant="contained"
            color="primary"
            startIcon={<DownloadOutlinedIcon />}
            onClick={handleDownload}
          >
            Download .txt File
          </VetanButton>
        </Box>
      </DialogActions>
    </Dialog>
  );
}
