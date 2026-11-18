import React, { useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
  Box,
  Drawer,
  AppBar,
  Toolbar,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Typography,
  IconButton,
  Avatar,
  Chip,
  Tooltip,
} from '@mui/material';
import DashboardOutlinedIcon from '@mui/icons-material/DashboardOutlined';
import PeopleOutlineIcon from '@mui/icons-material/PeopleOutline';
import AccessTimeOutlinedIcon from '@mui/icons-material/AccessTimeOutlined';
import EventNoteOutlinedIcon from '@mui/icons-material/EventNoteOutlined';
import PaymentsOutlinedIcon from '@mui/icons-material/PaymentsOutlined';
import VerifiedUserOutlinedIcon from '@mui/icons-material/VerifiedUserOutlined';
import BusinessOutlinedIcon from '@mui/icons-material/BusinessOutlined';
import BadgeOutlinedIcon from '@mui/icons-material/BadgeOutlined';
import LogoutOutlinedIcon from '@mui/icons-material/LogoutOutlined';
import MenuOutlinedIcon from '@mui/icons-material/MenuOutlined';

import { useAuth } from '../../context/AuthContext';
import BranchSelector from '../domain/BranchSelector';

const DRAWER_WIDTH = 240;

const NAV_ITEMS = [
  { label: 'Dashboard', path: '/dashboard', icon: DashboardOutlinedIcon, roles: ['ROLE_FOUNDER', 'ROLE_HR', 'ROLE_FINANCE', 'ROLE_CA'] },
  { label: 'Employee Portal', path: '/ess', icon: BadgeOutlinedIcon, roles: ['ROLE_EMPLOYEE', 'ROLE_FOUNDER', 'ROLE_HR'] },
  { label: 'Employees', path: '/employees', icon: PeopleOutlineIcon, roles: ['ROLE_FOUNDER', 'ROLE_HR'] },
  { label: 'Attendance', path: '/attendance', icon: AccessTimeOutlinedIcon, roles: ['ROLE_FOUNDER', 'ROLE_HR'] },
  { label: 'Leaves', path: '/leaves', icon: EventNoteOutlinedIcon, roles: ['ROLE_EMPLOYEE', 'ROLE_FOUNDER', 'ROLE_HR'] },
  { label: 'Payroll', path: '/payroll', icon: PaymentsOutlinedIcon, roles: ['ROLE_FOUNDER', 'ROLE_FINANCE'] },
  { label: 'CA & Compliance', path: '/ca-workspace', icon: VerifiedUserOutlinedIcon, roles: ['ROLE_FOUNDER', 'ROLE_FINANCE', 'ROLE_CA'] },
  { label: 'Branches', path: '/branches', icon: BusinessOutlinedIcon, roles: ['ROLE_FOUNDER', 'ROLE_HR'] },
];

export default function AppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [mobileOpen, setMobileOpen] = useState(false);

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen);
  };

  const userRoles = Array.isArray(user?.roles) && user.roles.length > 0
    ? user.roles
    : [user?.role || 'ROLE_FOUNDER'];

  const visibleNavItems = NAV_ITEMS.filter((item) => {
    if (item.roles.includes('ALL')) return true;
    return item.roles.some((r) => userRoles.includes(r));
  });

  const drawerContent = (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column', backgroundColor: '#090D16' }}>
      <Box sx={{ p: 2.5, display: 'flex', alignItems: 'center', gap: 1.5 }}>
        <img src="/brand/vetan-icon.svg" alt="Vetan Logo" style={{ width: 28, height: 28 }} />
        <Box>
          <Typography variant="h6" sx={{ fontWeight: 800, letterSpacing: '-0.02em', color: '#F8FAFC', lineHeight: 1 }}>
            Vetan
          </Typography>
          <Typography variant="caption" sx={{ color: '#64748B', fontSize: '0.68rem', letterSpacing: '0.04em' }}>
            PAYROLL & COMPLIANCE
          </Typography>
        </Box>
      </Box>

      <Box sx={{ px: 2, py: 1 }}>
        <Typography variant="caption" sx={{ px: 1, color: '#64748B', fontWeight: 600, fontSize: '0.68rem', letterSpacing: '0.05em', textTransform: 'uppercase' }}>
          Workspace
        </Typography>
      </Box>

      <List sx={{ px: 1.5, py: 0.5, flex: 1 }}>
        {visibleNavItems.map((item) => {
          const Icon = item.icon;
          const isActive = location.pathname.startsWith(item.path);
          return (
            <ListItem key={item.path} disablePadding sx={{ mb: 0.5 }}>
              <ListItemButton
                onClick={() => {
                  navigate(item.path);
                  setMobileOpen(false);
                }}
                sx={{
                  borderRadius: '6px',
                  py: 1,
                  px: 1.5,
                  backgroundColor: isActive ? 'rgba(99, 102, 241, 0.12)' : 'transparent',
                  border: isActive ? '1px solid rgba(99, 102, 241, 0.25)' : '1px solid transparent',
                  '&:hover': {
                    backgroundColor: isActive ? 'rgba(99, 102, 241, 0.16)' : 'rgba(255, 255, 255, 0.03)',
                  },
                }}
              >
                <ListItemIcon sx={{ minWidth: 32, color: isActive ? 'primary.light' : '#94A3B8' }}>
                  <Icon sx={{ fontSize: 20 }} />
                </ListItemIcon>
                <ListItemText
                  primary={item.label}
                  primaryTypographyProps={{
                    fontSize: '0.84rem',
                    fontWeight: isActive ? 600 : 500,
                    color: isActive ? '#F8FAFC' : '#94A3B8',
                  }}
                />
              </ListItemButton>
            </ListItem>
          );
        })}
      </List>

      <Box sx={{ p: 2, borderTop: '1px solid rgba(255, 255, 255, 0.06)' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25 }}>
            <Avatar sx={{ width: 32, height: 32, fontSize: '0.8rem', bgcolor: '#4F46E5' }}>
              {(user?.fullName || user?.email || 'U').charAt(0).toUpperCase()}
            </Avatar>
            <Box sx={{ maxWidth: 110, overflow: 'hidden' }}>
              <Typography variant="body2" sx={{ fontWeight: 600, color: '#F8FAFC', noWrap: true }}>
                {user?.fullName || 'User'}
              </Typography>
              <Chip
                label={(user?.role || 'FOUNDER').replace('ROLE_', '')}
                size="small"
                sx={{
                  height: 16,
                  fontSize: '0.62rem',
                  px: 0.25,
                  backgroundColor: 'rgba(255, 255, 255, 0.06)',
                  color: '#94A3B8',
                }}
              />
            </Box>
          </Box>
          <Tooltip title="Log out">
            <IconButton aria-label="Log out" size="small" onClick={logout} sx={{ color: '#94A3B8', '&:hover': { color: '#EF4444' } }}>
              <LogoutOutlinedIcon sx={{ fontSize: 18 }} />
            </IconButton>
          </Tooltip>
        </Box>
      </Box>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', backgroundColor: '#090D16' }}>
      <AppBar
        position="fixed"
        sx={{
          width: { sm: `calc(100% - ${DRAWER_WIDTH}px)` },
          ml: { sm: `${DRAWER_WIDTH}px` },
          backgroundColor: '#0F172A',
          borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
          boxShadow: 'none',
        }}
      >
        <Toolbar sx={{ justifyContent: 'space-between', minHeight: 64 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <IconButton aria-label="Open navigation drawer" color="inherit" edge="start" onClick={handleDrawerToggle} sx={{ mr: 1, display: { sm: 'none' } }}>
              <MenuOutlinedIcon />
            </IconButton>
            <BranchSelector />
          </Box>
        </Toolbar>
      </AppBar>

      <Box component="nav" sx={{ width: { sm: DRAWER_WIDTH }, flexShrink: { sm: 0 } }}>
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={handleDrawerToggle}
          ModalProps={{ keepMounted: true }}
          sx={{
            display: { xs: 'block', sm: 'none' },
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: DRAWER_WIDTH, borderRight: '1px solid rgba(255, 255, 255, 0.08)' },
          }}
        >
          {drawerContent}
        </Drawer>
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: 'none', sm: 'block' },
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: DRAWER_WIDTH, borderRight: '1px solid rgba(255, 255, 255, 0.08)' },
          }}
          open
        >
          {drawerContent}
        </Drawer>
      </Box>

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
          width: { sm: `calc(100% - ${DRAWER_WIDTH}px)` },
          mt: 8,
          backgroundColor: '#090D16',
          minHeight: 'calc(100vh - 64px)',
        }}
      >
        <Outlet />
      </Box>
    </Box>
  );
}
