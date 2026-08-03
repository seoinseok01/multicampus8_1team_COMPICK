const button = document.querySelector('#payment-button');
const data = document.body.dataset;

if (button && data.clientKey) {
  try {
    const payment = TossPayments(data.clientKey).payment({ customerKey: data.customerKey });
    button.addEventListener('click', async () => {
      button.disabled = true;
      try {
        await payment.requestPayment({
          method: 'CARD',
          amount: { currency: 'KRW', value: Number(data.amount) },
          orderId: data.orderId,
          orderName: data.orderName,
          customerName: data.customerName,
          successUrl: `${location.origin}/payments/success`,
          failUrl: `${location.origin}/payments/fail`,
          card: { flowMode: 'DEFAULT' }
        });
      } catch (error) {
        if (error.code !== 'USER_CANCEL') alert(error.message || '결제창을 열지 못했습니다.');
        button.disabled = false;
      }
    });
  } catch (error) {
    button.disabled = true;
    alert(error.message || '토스 결제를 초기화하지 못했습니다.');
  }
}
