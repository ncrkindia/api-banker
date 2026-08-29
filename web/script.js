document.addEventListener('DOMContentLoaded', () => {
    // Scroll Animations
    const scrollObserver = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('is-visible');
            }
        });
    }, {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    });

    const animateElements = document.querySelectorAll('.animate-on-scroll');
    animateElements.forEach(el => scrollObserver.observe(el));

    // Active Navigation Highlight
    const currentLocation = location.pathname.split('/').pop() || 'index.html';
    const navLinks = document.querySelectorAll('.nav-links a:not(.btn)');
    navLinks.forEach(link => {
        const linkPath = link.getAttribute('href');
        if (linkPath === currentLocation) {
            link.style.color = 'var(--accent-1)';
        }
    });

    // Mobile Menu Toggle
    const mobileMenuToggle = document.querySelector('.mobile-menu-toggle');
    const mainNav = document.querySelector('.main-nav');
    if (mobileMenuToggle && mainNav) {
        mobileMenuToggle.addEventListener('click', () => {
            mainNav.classList.toggle('is-open');
        });

        // Close menu when clicking outside
        document.addEventListener('click', (e) => {
            if (!mobileMenuToggle.contains(e.target) && !mainNav.contains(e.target) && mainNav.classList.contains('is-open')) {
                mainNav.classList.remove('is-open');
            }
        });
    }

    // Scroll to Top Button
    const scrollToTopBtn = document.getElementById('scrollToTop');
    if (scrollToTopBtn) {
        window.addEventListener('scroll', () => {
            if (window.scrollY > 500) {
                scrollToTopBtn.classList.add('is-visible');
            } else {
                scrollToTopBtn.classList.remove('is-visible');
            }
        });

        scrollToTopBtn.addEventListener('click', () => {
            window.scrollTo({
                top: 0,
                behavior: 'smooth'
            });
        });
    }

    // Guide Navigation Highlight
    const guideLinks = document.querySelectorAll('.guide-nav a');
    if (guideLinks.length > 0) {
        const sections = document.querySelectorAll('.guide-content-section');
        
        // Handle click on guide links for smooth scroll and offset
        guideLinks.forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                const targetId = link.getAttribute('href');
                const targetSection = document.querySelector(targetId);
                if (targetSection) {
                    targetSection.scrollIntoView({ behavior: 'smooth' });
                    // Optional: update URL hash without scrolling
                    history.pushState(null, null, targetId);
                }
            });
        });

        const guideObserver = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    guideLinks.forEach(link => {
                        link.classList.remove('active');
                        if (link.getAttribute('href') === '#' + entry.target.id) {
                            link.classList.add('active');
                        }
                    });
                }
            });
        }, { 
            threshold: 0.2,
            rootMargin: '-100px 0px 0px 0px' 
        });

        sections.forEach(section => guideObserver.observe(section));
    }

    // Download page Support Modal Logic
    const downloadLinks = document.querySelectorAll('a[href*="github.com/ncrkindia/api-banker/releases/download"]');
    const supportModal = document.getElementById('supportModal');
    if (downloadLinks.length > 0 && supportModal) {
        const closeModal = document.getElementById('closeModal');
        const maybeLaterBtn = document.getElementById('maybeLaterBtn');

        const hideModal = () => {
            supportModal.classList.remove('active');
        };

        if (closeModal) closeModal.addEventListener('click', hideModal);
        if (maybeLaterBtn) maybeLaterBtn.addEventListener('click', hideModal);
        
        // Close on outside click
        supportModal.addEventListener('click', (e) => {
            if (e.target === supportModal) hideModal();
        });

        downloadLinks.forEach(link => {
            link.addEventListener('click', (e) => {
                // Let the browser handle the actual download file normally
                // Trigger the modal after 3 seconds
                setTimeout(() => {
                    supportModal.classList.add('active');
                }, 3000);
            });
        });
    }
});
