import React, { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Box,
  Typography,
  IconButton,
  Select,
  MenuItem,
  Tooltip,
} from '@mui/material';
import CloseOutlinedIcon from '@mui/icons-material/CloseOutlined';
import DownloadOutlinedIcon from '@mui/icons-material/DownloadOutlined';
import VetanButton from '../ui/VetanButton';

const BANK_FORMATS = [
  { id: 'HDFC', name: 'HDFC Bank Corporate NetBanking (CSV)' },
  { id: 'ICICI', name: 'ICICI Bank Corporate Payout (CSV)' },
  { id: 'SBI', name: 'State Bank of India CMP (CSV)' },
  { id: 'AXIS', name: 'Axis Bank Salary Upload (CSV)' },
];

export default function BankExportModal({
  open = false,
  onClose,
  onExport,
  runId,
  loading = false,
}) {
  const [selectedBank, setSelectedBank] = useState('HDFC');

  const handleDownload = () => {
    if (onExport) {
      onExport(selectedBank);
    }
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="sm"
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
            Bank Salary Disbursement CSV
          </Typography>
          <Typography variant="caption" sx={{ color: 'text.secondary' }}>
            Generate bank-compliant batch payout file for corporate portal upload
          </Typography>
        </Box>
        <Tooltip title="Close modal">
          <IconButton aria-label="Close Bank Export modal" size="small" onClick={onClose} sx={{ color: 'text.secondary' }}>
            <CloseOutlinedIcon sx={{ fontSize: 18 }} />
          </IconButton>
        </Tooltip>
      </DialogTitle>

      <DialogContent sx={{ py: 3 }}>
        <Typography variant="subtitle2" sx={{ fontWeight: 600, mb: 1, color: 'text.primary' }}>
          Select Bank Batch Format
        </Typography>
        <Select
          fullWidth
          size="small"
          value={selectedBank}
          onChange={(e) => setSelectedBank(e.target.value)}
          sx={{
            backgroundColor: '#1E293B',
            borderRadius: '6px',
            fontSize: '0.875rem',
            '& fieldset': { borderColor: 'rgba(255, 255, 255, 0.12)' },
            mb: 2.5,
          }}
        >
          {BANK_FORMATS.map((b) => (
            <MenuItem key={b.id} value={b.id} sx={{ fontSize: '0.875rem' }}>
              {b.name}
            </MenuItem>
          ))}
        </Select>

        <Box sx={{ p: 2, backgroundColor: '#090D16', borderRadius: '6px', border: '1px solid rgba(255, 255, 255, 0.08)' }}>
          <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block', mb: 0.5 }}>
            File Format Specification:
          </Typography>
          <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: '0.75rem', color: '#818CF8' }}>
            Beneficiary Name, Bank Account No, IFSC Code, Net Pay, Narration
          </Typography>
        </Box>
      </DialogContent>

      <DialogActions sx={{ px: 3, py: 2, borderTop: '1px solid rgba(255, 255, 255, 0.08)' }}>
        <VetanButton variant="outlined" onClick={onClose}>
          Cancel
        </VetanButton>
        <VetanButton
          variant="contained"
          color="primary"
          startIcon={<DownloadOutlinedIcon />}
          loading={loading}
          onClick={handleDownload}
        >
          Download CSV
        </VetanButton>
      </DialogActions>
    </Dialog>
  );
}
