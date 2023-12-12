function scrollToElement(element) {
  if (element) {
    element.scrollIntoView({ behavior: 'smooth' });
  }
}

document.querySelectorAll('h1, h2, h3, h4, h5').forEach(heading => {
  heading.addEventListener('click', function() {
    const id = this.getAttribute('id');
    if (id) {
      const newUrl = `${window.location.pathname}#${id}`;
      window.history.pushState({ path: newUrl }, '', newUrl);
      scrollToElement(this);
    }
  });
});

// Smooth scroll on page load if URL contains a hash
window.onload = () => {
  const hash = window.location.hash;
  if (hash) {
    const escapedHash = hash.replace(/(:|\.|\[|\]|,|=|@)/g, "\\$1");
    const targetElement = document.querySelector(escapedHash);
    scrollToElement(targetElement);
  }
};

const sidebar = document.getElementById("post");

if (localStorage.getItem("post-scroll") !== null) {
  sidebar.scrollTop = parseInt(localStorage.getItem("post-scroll"), 10);
}

window.addEventListener("beforeunload", () => {
  localStorage.setItem("post-scroll", sidebar.scrollTop);
});
