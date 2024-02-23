[...document.querySelectorAll("article img:not(.no-spotlight)")].map((elem) =>
  elem.classList.add("spotlight"),
);
