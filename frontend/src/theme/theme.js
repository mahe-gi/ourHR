import { createTheme } from '@mui/material/styles';

const theme = createTheme({
  palette: {
    mode: 'dark',
    primary: {
      main: '#6366F1',
      dark: '#4F46E5',
      light: '#818CF8',
      contrastText: '#FFFFFF',
    },
    secondary: {
      main: '#94A3B8',
      dark: '#64748B',
      light: '#F8FAFC',
    },
    success: {
      main: '#10B981',
      dark: '#059669',
      light: '#34D399',
    },
    warning: {
      main: '#F59E0B',
      dark: '#D97706',
    },
    error: {
      main: '#EF4444',
      dark: '#DC2626',
    },
    background: {
      default: '#090D16',
      paper: '#0F172A',
    },
    text: {
      primary: '#F8FAFC',
      secondary: '#94A3B8',
      disabled: '#64748B',
    },
    divider: 'rgba(255, 255, 255, 0.08)',
    action: {
      hover: 'rgba(255, 255, 255, 0.04)',
      selected: 'rgba(99, 102, 241, 0.12)',
    },
  },
  typography: {
    fontFamily: [
      '"Plus Jakarta Sans"',
      '-apple-system',
      'BlinkMacSystemFont',
      '"Segoe UI"',
      'Roboto',
      'sans-serif',
    ].join(','),
    h1: { fontSize: '2rem', fontWeight: 700, letterSpacing: '-0.025em' },
    h2: { fontSize: '1.5rem', fontWeight: 600, letterSpacing: '-0.02em' },
    h3: { fontSize: '1.25rem', fontWeight: 600, letterSpacing: '-0.015em' },
    h4: { fontSize: '1.125rem', fontWeight: 600 },
    h5: { fontSize: '1rem', fontWeight: 600 },
    h6: { fontSize: '0.875rem', fontWeight: 600 },
    subtitle1: { fontSize: '0.9375rem', color: '#94A3B8' },
    subtitle2: { fontSize: '0.8125rem', color: '#64748B', fontWeight: 500 },
    body1: { fontSize: '0.875rem', lineHeight: 1.5 },
    body2: { fontSize: '0.8125rem', color: '#94A3B8' },
    button: { textTransform: 'none', fontWeight: 600 },
  },
  shape: {
    borderRadius: 8,
  },
  components: {
    MuiCssBaseline: {
      styleOverrides: {
        body: {
          backgroundColor: '#090D16',
          color: '#F8FAFC',
          scrollbarColor: '#334155 #090D16',
        },
      },
    },
    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          transition: 'all 0.15s cubic-bezier(0.4, 0, 0.2, 1)',
          '&:active': {
            transform: 'scale(0.98)',
          },
        },
        containedPrimary: {
          background: 'linear-gradient(180deg, #6366F1 0%, #4F46E5 100%)',
          boxShadow: '0 1px 2px rgba(0, 0, 0, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.15)',
          '&:hover': {
            background: 'linear-gradient(180deg, #818CF8 0%, #6366F1 100%)',
          },
        },
        outlined: {
          borderColor: 'rgba(255, 255, 255, 0.12)',
          '&:hover': {
            borderColor: 'rgba(255, 255, 255, 0.25)',
            backgroundColor: 'rgba(255, 255, 255, 0.04)',
          },
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundImage: 'none',
          backgroundColor: '#0F172A',
          border: '1px solid rgba(255, 255, 255, 0.08)',
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          backgroundColor: '#0F172A',
          border: '1px solid rgba(255, 255, 255, 0.08)',
          borderRadius: 10,
        },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        root: {
          borderBottom: '1px solid rgba(255, 255, 255, 0.06)',
          padding: '12px 16px',
          fontSize: '0.84rem',
        },
        head: {
          backgroundColor: '#1E293B',
          color: '#94A3B8',
          fontWeight: 600,
          fontSize: '0.75rem',
          textTransform: 'uppercase',
          letterSpacing: '0.05em',
        },
      },
    },
    MuiTableRow: {
      styleOverrides: {
        root: {
          '&:hover': {
            backgroundColor: 'rgba(255, 255, 255, 0.02)',
          },
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: {
          fontWeight: 600,
          fontSize: '0.75rem',
          borderRadius: 6,
        },
      },
    },
  },
});

export default theme;
