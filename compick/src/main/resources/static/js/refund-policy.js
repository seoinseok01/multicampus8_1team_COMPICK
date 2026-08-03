(() => {
    const modal = document.getElementById('refund-policy-modal');
    const confirmButton = document.getElementById('policy-confirm');
    if (!modal || !confirmButton) return;
    const description = [...modal.querySelectorAll('.policy-dialog > p')]
        .find(element => element.textContent.includes('50%'));
    if (!description) return;

    const warning = document.createElement('div');
    warning.className = 'half-refund-warning';
    warning.innerHTML = '<strong>결제금액의 50%만 환불됩니다.</strong><span>나머지 50%는 환불되지 않으니 환불 예정 금액을 반드시 확인해 주세요.</span>';
    description.after(warning);

    const consent = document.createElement('label');
    consent.className = 'half-refund-consent';
    consent.innerHTML = '<input type="checkbox"> <span>결제금액의 50% 환불 정책에 동의합니다.</span>';
    modal.querySelector('.policy-actions').before(consent);
    const checkbox = consent.querySelector('input');
    confirmButton.disabled = true;
    checkbox.addEventListener('change', () => confirmButton.disabled = !checkbox.checked);
    confirmButton.addEventListener('click', event => {
        if (checkbox.checked) return;
        event.preventDefault();
        event.stopImmediatePropagation();
        checkbox.focus();
    }, true);
})();
