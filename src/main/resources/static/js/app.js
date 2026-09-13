document.addEventListener('DOMContentLoaded', function () {
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
