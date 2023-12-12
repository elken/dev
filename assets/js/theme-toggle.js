var themeDarkIcon = document.getElementById("theme-toggle-dark");
var themeLightIcon = document.getElementById("theme-toggle-light");

function sendMessage(message) {
    const iframe = document.querySelector('iframe.giscus-frame');
    if (!iframe) return;
    iframe.contentWindow.postMessage({ giscus: message }, 'https://giscus.app');
}

function togglePrismTheme(darkMode) {
    const themeLink = document.getElementById('prism-theme');
    if (darkMode) {
        themeLink.href = 'https://raw.githack.com/PrismJS/prism-themes/master/themes/prism-one-dark.css';
    } else {
        themeLink.href = 'https://raw.githack.com/PrismJS/prism-themes/master/themes/prism-one-light.css';
    }
}

function changeTheme() {
    // toggle icons inside button
    themeDarkIcon.classList.toggle('hidden');
    themeLightIcon.classList.toggle('hidden');

    // if set via local storage previously
    if (localStorage.getItem('color-theme')) {
        if (localStorage.getItem('color-theme') === 'light') {
            document.documentElement.classList.add('dark');
            localStorage.setItem('color-theme', 'dark');
            togglePrismTheme(true)
        } else {
            document.documentElement.classList.remove('dark');
            localStorage.setItem('color-theme', 'light');
            togglePrismTheme(false)
        }

        // if NOT set via local storage previously
    } else {
        if (document.documentElement.classList.contains('dark')) {
            document.documentElement.classList.remove('dark');
            localStorage.setItem('color-theme', 'light');
            togglePrismTheme(false)
        } else {
            document.documentElement.classList.add('dark');
            localStorage.setItem('color-theme', 'dark');
            togglePrismTheme(true)
        }
    }

    sendMessage({
        setConfig: {
            theme: localStorage.getItem('color-theme')
        }
    });
}

if (localStorage.getItem('color-theme') === 'dark' || (!('color-theme' in localStorage) && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
    themeLightIcon.classList.remove('hidden');
    document.documentElement.classList.add('dark');
    togglePrismTheme(true)
} else {
    document.documentElement.classList.remove('dark');
    themeDarkIcon.classList.remove('hidden');
    togglePrismTheme(false)
}
