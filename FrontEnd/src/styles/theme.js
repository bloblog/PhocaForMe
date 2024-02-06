import { createTheme } from '@mui/material/styles';

const theme = createTheme({
  palette: {
    primary: {
      main: '#FB37A3',
    },
    secondary: {
      main: '#FD9DD1',
    },
    warning: {
      main: 'rgba(142, 96, 203, 1)',
    },
    info: {
        main: '#FFFFFF'
    },
    success: {
      main: 'rgba(135, 141, 150, 1)'
    }

  },

  components: {
    MuiContainer: {
      styleOverrides: {
        root: {
          
          width: '85vw',
          height: '85vh',
        }
      }
    },
    MuiMenu: {
      styleOverrides: {
        paper: {
          backgroundColor: '#FD9DD1', 
          width: '10rem'
        },
      },
    },
    MuiListItemText: {
      styleOverrides: {
        secondary: {
          fontSize: '14px',
          fontFamily: 'Pretendard'
        }

      }
    },
    MuiFormControlLabel: {
      styleOverrides: {
        label: {
          fontFamily: 'Pretendard'
        }
      }
    },
    MuiTypography: {
      styleOverrides: {
        root: {
          fontFamily: 'Pretendard'
        }
      }
    }, 
    MuiTab: {
      styleOverrides: {
        root: {
          fontFamily: 'Pretendard',
          padding: '0rem 1rem',
        }
      }
    },
    MuiCardHeader: {
      styleOverrides: {
        title: {
          fontSize: '18px'
        }
      }
    },
    MuiButton: {
      styleOverrides: {
        root: {
          fontFamily: 'Cafe24SSurround',
        }
        
      }
    },
    MuiTextField: {
      styleOverrides: {
        root: {
          '& input': {
            fontFamily: 'Pretendard',
            fontSize: '15px',
          },
          
        },
      }
    },
    
  
  }
});

export default theme;
