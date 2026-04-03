document.addEventListener('DOMContentLoaded', () => {
  // --- 0. Прелоадер ---
  const preloader = document.querySelector('.js-preloader');
  if (preloader) {
    window.addEventListener('load', () => {
      // Небольшая задержка, чтобы анимация успела проиграться
      setTimeout(() => {
        preloader.classList.add('preloader--hidden');
      }, 500); // 500ms delay to ensure it stays visible a bit even if page loads instantly
    });
  }

  // --- 1. Липкая шапка (Sticky Header) и Подсветка активного пункта меню ---
  const header = document.querySelector('.js-header');
  const sections = document.querySelectorAll('section[id]');
  const navLinks = document.querySelectorAll('.header__nav-link');

  const handleScroll = () => {
    // Липкая шапка
    if (window.scrollY > 10) {
      header.classList.add('header--scrolled');
    } else {
      header.classList.remove('header--scrolled');
    }

    // Подсветка активного пункта при скролле
    let currentSectionId = '';
    const scrollPosition = window.scrollY + 100; // Добавляем отступ (чуть больше высоты шапки)

    sections.forEach(section => {
      const sectionTop = section.offsetTop;
      const sectionHeight = section.offsetHeight;

      if (scrollPosition >= sectionTop && scrollPosition < sectionTop + sectionHeight) {
        currentSectionId = section.getAttribute('id');
      }
    });

    // Если мы доскроллили до самого низа (для футера / последних блоков)
    if ((window.innerHeight + window.scrollY) >= document.body.offsetHeight - 50) {
      if (sections.length > 0) {
        currentSectionId = sections[sections.length - 1].getAttribute('id');
      }
    }

    const scrollSpyLinks = document.querySelectorAll('.js-scroll-link');
    scrollSpyLinks.forEach(link => {
      link.classList.remove('header__nav-link--active');
      const href = link.getAttribute('href');
      const hashIndex = href ? href.indexOf('#') : -1;
      if (hashIndex !== -1) {
        const hash = href.substring(hashIndex + 1);
        if (hash === currentSectionId && currentSectionId !== '') {
          link.classList.add('header__nav-link--active');
        }
      }
    });
  };

  window.addEventListener('scroll', handleScroll);
  handleScroll(); // инициализация

  // --- 2. Мобильное бургер-меню ---
  const burgerBtn = document.querySelector('.js-burger-btn');
  const nav = document.querySelector('.js-nav');

  // Закрытие меню при клике на бургер
  burgerBtn.addEventListener('click', () => {
    burgerBtn.classList.toggle('header__burger--active');
    nav.classList.toggle('header__nav--open');
  });

  // Закрытие меню при клике по ссылке (Активное состояние теперь обрабатывается в handleScroll)
  navLinks.forEach(link => {
    link.addEventListener('click', () => {
      // Закрываем мобильное меню
      burgerBtn.classList.remove('header__burger--active');
      nav.classList.remove('header__nav--open');
    });
  });

  // --- 3. Плавная прокрутка ---
  const scrollLinks = document.querySelectorAll('.js-scroll-link');
  scrollLinks.forEach(link => {
    link.addEventListener('click', (e) => {
      e.preventDefault();
      const targetId = link.getAttribute('href');
      const targetEl = document.querySelector(targetId);
      if (targetEl) {
        // Отступ для фиксированной шапки
        const headerOffset = 80;
        const elementPosition = targetEl.getBoundingClientRect().top;
        let offsetPosition = elementPosition + window.pageYOffset - headerOffset;

        // 150px additional scroll offset for #advantages section
        if (targetId === '#advantages') {
          offsetPosition += 150;
        }

        window.scrollTo({
          top: offsetPosition,
          behavior: 'smooth'
        });
      }
    });
  });

  // --- 3.1. Плавная прокрутка наверх по клику на логотип ---
  const logoLink = document.querySelector('.header__logo');
  if (logoLink) {
    logoLink.addEventListener('click', (e) => {
      // Проверяем, находимся ли мы на главной странице
      const isHomePage = window.location.pathname === '/' || window.location.pathname.endsWith('index.html') || window.location.pathname.endsWith('ADV/') || window.location.pathname === '';

      if (isHomePage || window.location.hash) {
        e.preventDefault();
        window.scrollTo({
          top: 0,
          behavior: 'smooth'
        });

        // Сбрасываем активные классы у ссылок
        document.querySelectorAll('.header__nav-link').forEach(l => l.classList.remove('header__nav-link--active'));

        // Очищаем хэш в URL
        if (window.history.pushState) {
          window.history.pushState('', '/', window.location.pathname);
        } else {
          window.location.hash = '';
        }
      }
    });
  }

  // --- 4. Модальное окно с изображением (Попап) ---
  const popup = document.querySelector('.js-popup');
  const popupImg = document.querySelector('.js-popup-img');
  const popupCloseBtns = document.querySelectorAll('.js-popup-close');
  const popupTriggers = document.querySelectorAll('.js-popup-trigger');

  popupTriggers.forEach(el => {
    el.addEventListener('click', () => {
      const originSrc = el.getAttribute('data-origin');
      if (originSrc) {
        popupImg.src = originSrc;
        popup.classList.add('popup--active');
        document.body.style.overflow = 'hidden'; // предотвращаем прокрутку фона
      }
    });
  });

  const closePopup = () => {
    popup.classList.remove('popup--active');

    // Check if there is also payment popup
    const paymentPopup = document.getElementById('payment-popup');
    if (paymentPopup) {
      paymentPopup.classList.remove('popup--active');

      // Reset payment form state after closing
      setTimeout(() => {
        const checkboxes = paymentPopup.querySelectorAll('.js-payment-checkbox');
        checkboxes.forEach(cb => cb.checked = false);
        const submitBtn = paymentPopup.querySelector('.js-payment-submit');
        const errorMsg = paymentPopup.querySelector('.js-payment-error');
        if (submitBtn) submitBtn.classList.add('is-disabled');
        if (errorMsg) errorMsg.classList.remove('payment-form__error--visible');
      }, 300);
    }

    document.body.style.overflow = '';
    // небольшая задержка перед очисткой src для избежания мерцания во время CSS-перехода
    setTimeout(() => {
      if (popupImg) popupImg.src = '';
    }, 300);
  };

  popupCloseBtns.forEach(btn => {
    btn.addEventListener('click', closePopup);
  });

  // Закрытие по нажатию клавиши Escape
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      if (popup && popup.classList.contains('popup--active')) closePopup();

      const paymentPopup = document.getElementById('payment-popup');
      if (paymentPopup && paymentPopup.classList.contains('popup--active')) closePopup();
    }
  });

  // --- 4.1 Payment Popup Logic ---
  const paymentTriggers = document.querySelectorAll('.js-payment-trigger');
  const paymentPopup = document.getElementById('payment-popup');

  if (paymentTriggers.length > 0 && paymentPopup) {
    const amountSpan = paymentPopup.querySelector('.js-payment-amount');
    const priceSpan = paymentPopup.querySelector('.js-payment-price');
    const checkboxes = paymentPopup.querySelectorAll('.js-payment-checkbox');
    const submitBtn = paymentPopup.querySelector('.js-payment-submit');
    const errorMsg = paymentPopup.querySelector('.js-payment-error');

    // Открытие попапа
    paymentTriggers.forEach(trigger => {
      trigger.addEventListener('click', (e) => {
        e.preventDefault();

        // Подстановка данных
        const amount = trigger.getAttribute('data-amount');
        const price = trigger.getAttribute('data-price');

        if (amountSpan) amountSpan.textContent = amount;
        if (priceSpan) priceSpan.textContent = price;

        // Показ попапа
        paymentPopup.classList.add('popup--active');
        document.body.style.overflow = 'hidden';
      });
    });

    // Валидация чекбоксов
    const validateCheckboxes = () => {
      let allChecked = true;
      checkboxes.forEach(cb => {
        if (!cb.checked) {
          allChecked = false;
        }
      });

      if (allChecked) {
        submitBtn.classList.remove('is-disabled');
        errorMsg.classList.remove('payment-form__error--visible');
      } else {
        submitBtn.classList.add('is-disabled');
      }
    };

    checkboxes.forEach(cb => {
      cb.addEventListener('change', validateCheckboxes);
    });

    // Клик по неактивной кнопке оплаты (для показа ошибки)
    // Так как disabled кнопка не вызывает click events в некоторых браузерах, 
    // оборачиваем её в дополнительную логику или слушаем клик на контейнере
    const actionContainer = paymentPopup.querySelector('.payment-form__action');
    if (actionContainer) {
      actionContainer.addEventListener('click', (e) => {
        // Проверяем, если клик был по отключенной кнопке
        if (submitBtn.classList.contains('is-disabled') && (e.target === submitBtn || submitBtn.contains(e.target))) {
          e.preventDefault();
          errorMsg.classList.add('payment-form__error--visible');
        } else if (!submitBtn.classList.contains('is-disabled') && (e.target === submitBtn || submitBtn.contains(e.target))) {
          // Успешная отправка (Здесь будет обращение к бэкенду или платежному шлюзу)
          console.log('Переход к оплате...');
          // window.location.href = 'ссылка_на_оплату';
        }
      });
    }
  }



  // --- 5. Анимация появления при прокрутке (Блок Преимущества) ---
  const observerOptions = {
    root: null,
    rootMargin: '0px',
    threshold: 0.1
  };

  const advantagesObserver = new IntersectionObserver((entries, observer) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        const items = entry.target.querySelectorAll('.advantages__item');
        items.forEach((item, index) => {
          setTimeout(() => {
            item.classList.add('advantages__item--visible');
          }, index * 200); // задержка для каскадного эффекта
        });
        observer.unobserve(entry.target);
      }
    });
  }, observerOptions);

  const advantagesList = document.querySelector('.js-advantages-list');
  if (advantagesList) {
    advantagesObserver.observe(advantagesList);
  }

  // --- 6. About Page: Copy to Clipboard ---
  const copyButtons = document.querySelectorAll('.about-docs__copy');

  copyButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      // Find the parent value container
      const parentValue = btn.closest('.about-docs__value');
      if (parentValue) {
        // Clone the node to manipulate text without affecting DOM
        const clone = parentValue.cloneNode(true);
        // Remove the button from the clone so we only get the text
        const btnInClone = clone.querySelector('.about-docs__copy');
        if (btnInClone) btnInClone.remove();

        const textToCopy = clone.textContent.trim();

        navigator.clipboard.writeText(textToCopy).then(() => {
          // Visual feedback
          const originalIcon = btn.innerHTML;
          // Simple check icon SVG
          btn.innerHTML = `<svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M13.3333 4L6 11.3333L2.66667 8" stroke="#1CA852" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>`;

          setTimeout(() => {
            btn.innerHTML = originalIcon;
          }, 2000);
        }).catch(err => {
          console.error('Failed to copy text: ', err);
        });
      }
    });
  });
});
