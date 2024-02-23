document
  .querySelectorAll("article img:not(.no-spotlight)")
  .forEach(function (elem) {
    elem.classList.add("spotlight");
  });
