(function () {
    'use strict';

    const THEME_KEY = 'staylanka-theme';
    const root = document.documentElement;
    const systemDark = window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null;
    const reducedMotion = window.matchMedia ? window.matchMedia('(prefers-reduced-motion: reduce)') : null;

    const readStoredTheme = () => {
        try {
            const value = localStorage.getItem(THEME_KEY);
            return value === 'light' || value === 'dark' ? value : null;
        } catch (ignored) {
            return null;
        }
    };

    const writeStoredTheme = (theme) => {
        try {
            localStorage.setItem(THEME_KEY, theme);
        } catch (ignored) {
            // Storage may be unavailable in privacy-restricted contexts; the active theme still works.
        }
    };

    const syncThemeControls = (theme) => {
        const isDark = theme === 'dark';
        document.querySelectorAll('[data-theme-toggle]').forEach((button) => {
            const nextTheme = isDark ? 'light' : 'dark';
            const label = `Switch to ${nextTheme} mode`;
            button.setAttribute('aria-pressed', String(isDark));
            button.setAttribute('aria-label', label);
            button.setAttribute('title', label);
            const accessibleLabel = button.querySelector('[data-theme-label]');
            if (accessibleLabel) accessibleLabel.textContent = label;
        });
    };

    const applyTheme = (theme, persist) => {
        root.dataset.theme = theme;
        root.setAttribute('data-bs-theme', theme);
        root.style.colorScheme = theme;

        const themeMeta = document.querySelector('meta[name="theme-color"]');
        if (themeMeta) themeMeta.setAttribute('content', theme === 'dark' ? '#050816' : '#f7f9fd');

        syncThemeControls(theme);
        if (persist) writeStoredTheme(theme);
    };

    const changeTheme = (nextTheme, button) => {
        const persist = true;
        const canReveal = button
            && typeof document.startViewTransition === 'function'
            && !(reducedMotion && reducedMotion.matches);

        if (!canReveal) {
            applyTheme(nextTheme, persist);
            return;
        }

        const rect = button.getBoundingClientRect();
        const x = rect.left + rect.width / 2;
        const y = rect.top + rect.height / 2;
        const radius = Math.hypot(Math.max(x, window.innerWidth - x), Math.max(y, window.innerHeight - y));
        root.style.setProperty('--sl-theme-x', `${x}px`);
        root.style.setProperty('--sl-theme-y', `${y}px`);
        root.style.setProperty('--sl-theme-radius', `${radius}px`);

        document.startViewTransition(() => applyTheme(nextTheme, persist));
    };

    document.addEventListener('DOMContentLoaded', function () {
        // The inline head bootstrap already selected a theme before CSS loaded. Sync controls now that they exist.
        const main = document.querySelector('main');
        if (main && !main.id) main.id = 'main-content';
        const initialTheme = root.dataset.theme === 'dark' ? 'dark' : 'light';
        applyTheme(initialTheme, false);

        document.querySelectorAll('[data-theme-toggle]').forEach((button) => {
            button.addEventListener('click', () => {
                const currentTheme = root.dataset.theme === 'dark' ? 'dark' : 'light';
                changeTheme(currentTheme === 'dark' ? 'light' : 'dark', button);
            });
        });

        // Follow operating-system changes only until the user explicitly chooses a theme.
        if (systemDark) {
            const syncWithSystem = (event) => {
                if (!readStoredTheme()) applyTheme(event.matches ? 'dark' : 'light', false);
            };
            if (typeof systemDark.addEventListener === 'function') systemDark.addEventListener('change', syncWithSystem);
            else if (typeof systemDark.addListener === 'function') systemDark.addListener(syncWithSystem);
        }

        const navbar = document.querySelector('[data-navbar]');
        const updateNavbar = () => {
            if (navbar) navbar.classList.toggle('is-scrolled', window.scrollY > 12);
        };
        updateNavbar();
        window.addEventListener('scroll', updateNavbar, { passive: true });

        const colomboDateParts = new Intl.DateTimeFormat('en-US', {
            timeZone: 'Asia/Colombo', year: 'numeric', month: '2-digit', day: '2-digit'
        }).formatToParts(new Date()).reduce((parts, part) => {
            if (part.type !== 'literal') parts[part.type] = part.value;
            return parts;
        }, {});
        const localToday = `${colomboDateParts.year}-${colomboDateParts.month}-${colomboDateParts.day}`;

        document.querySelectorAll('[data-date-pair]').forEach(function (group) {
            const checkIn = group.querySelector('[data-check-in]');
            const checkOut = group.querySelector('[data-check-out]');
            if (!checkIn || !checkOut) return;

            checkIn.min = checkIn.min || localToday;
            checkOut.min = checkOut.min || localToday;

            const syncDates = () => {
                if (!checkIn.value) {
                    checkOut.min = localToday;
                    return;
                }
                const nextDay = new Date(checkIn.value + 'T00:00:00Z');
                nextDay.setUTCDate(nextDay.getUTCDate() + 1);
                const minCheckout = nextDay.toISOString().split('T')[0];
                checkOut.min = minCheckout;
                if (checkOut.value && checkOut.value < minCheckout) checkOut.value = '';
            };
            checkIn.addEventListener('change', syncDates);
            syncDates();
        });

        document.querySelectorAll('[data-password-toggle]').forEach(function (button) {
            button.addEventListener('click', function () {
                const target = document.getElementById(button.dataset.passwordToggle);
                if (!target) return;
                const showing = target.type === 'text';
                target.type = showing ? 'password' : 'text';
                button.setAttribute('aria-label', showing ? 'Show password' : 'Hide password');
                const icon = button.querySelector('i');
                if (icon) icon.className = showing ? 'bi bi-eye' : 'bi bi-eye-slash';
            });
        });

        document.querySelectorAll('form[data-submit-feedback]').forEach(function (form) {
            form.addEventListener('submit', function () {
                if (!form.checkValidity()) return;
                const button = form.querySelector('button[type="submit"]');
                if (!button) return;
                button.dataset.submitting = 'true';
                button.disabled = true;
            });
        });
    });

    document.addEventListener('click', function (event) {
        const button = event.target.closest('[data-confirm]');
        if (button && !window.confirm(button.dataset.confirm)) {
            event.preventDefault();
        }
    });
})();
