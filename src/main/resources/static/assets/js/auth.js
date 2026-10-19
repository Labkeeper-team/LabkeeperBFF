document.addEventListener('DOMContentLoaded', () => {
  const apiVersion = window.__LABKEEPER_API_VERSION;
  if (!apiVersion || apiVersion === 'IO_LABKEEPER_FRONTEND_API_VERSION') {
    return;
  }

  const loginButtons = document.querySelectorAll('.header__login');
  const projectsButtons = document.querySelectorAll('.header__projects');

  const setDisplay = (buttons, value) => {
    buttons.forEach(btn => {
      btn.style.display = value;
    });
  };

  fetch(`/api/${apiVersion}/public/user-info`, {
    method: 'GET',
    credentials: 'include'
  })
    .then(response => {
      if (!response.ok) {
        throw new Error(`user-info request failed: ${response.status}`);
      }
      return response.json();
    })
    .then(data => {
      if (data && data.isAuthenticated !== false) {
        setDisplay(loginButtons, 'none');
        setDisplay(projectsButtons, '');
      } else {
        setDisplay(loginButtons, '');
        setDisplay(projectsButtons, 'none');
      }
    })
    .catch(() => {
      // Keep login button visible on request errors.
    });
});
