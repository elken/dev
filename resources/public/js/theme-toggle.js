const themeDarkIcon = document.getElementById("theme-toggle-dark");
const themeLightIcon = document.getElementById("theme-toggle-light");

function sendMessage(message) {
    const iframe = document.querySelector('iframe.giscus-frame');
    if (!iframe) return;
    iframe.contentWindow.postMessage({ giscus: message }, 'https://giscus.app');
}

document.getElementById("theme-toggle").addEventListener("click",
    function () {
        // toggle icons inside button
        themeDarkIcon.classList.toggle('hidden');
        themeLightIcon.classList.toggle('hidden');

        // if set via local storage previously
        if (localStorage.getItem('color-theme')) {
            if (localStorage.getItem('color-theme') === 'light') {
                document.documentElement.classList.add('dark');
                localStorage.setItem('color-theme', 'dark');
            } else {
                document.documentElement.classList.remove('dark');
                localStorage.setItem('color-theme', 'light');
            }

            // if NOT set via local storage previously
        } else {
            if (document.documentElement.classList.contains('dark')) {
                document.documentElement.classList.remove('dark');
                localStorage.setItem('color-theme', 'light');
            } else {
                document.documentElement.classList.add('dark');
                localStorage.setItem('color-theme', 'dark');
            }
        }

        sendMessage({
            setConfig: {
                theme: localStorage.getItem('color-theme')
            }
        });
    })

if (localStorage.getItem('color-theme') === 'dark' || (!('color-theme' in localStorage) && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
    themeLightIcon.classList.remove('hidden');
    document.documentElement.classList.add('dark');
} else {
    document.documentElement.classList.remove('dark');
    themeDarkIcon.classList.remove('hidden');
}
