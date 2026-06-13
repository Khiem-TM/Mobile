const BASE_URL = 'http://localhost:3005';
const CLOUD_BASE = 'https://res.cloudinary.com/dl3lapqhm/raw/upload';

const exercises = [
  {
    name: 'Plank chữ T',
    muscleGroup: 'Bụng',
    difficultyLevel: 'INTERMEDIATE',
    metValue: 4.0,
    description: 'Bài tập plank kết hợp xoay thân tạo hình chữ T, tăng cường sức mạnh cơ bụng và vai.',
    instructions: 'Chống tay vào sàn ở tư thế plank, xoay thân nâng một tay thẳng lên cao tạo hình chữ T, giữ vài giây rồi đổi bên.',
    lottieAsset: `${CLOUD_BASE}/v1781322153/exercises/lottie/T-Plank.lottie`,
  },
  {
    name: 'Nhảy bục',
    muscleGroup: 'Chân',
    difficultyLevel: 'ADVANCED',
    metValue: 8.0,
    description: 'Bài tập nhảy bật người lên bục cao, phát triển sức mạnh bùng nổ cho đôi chân.',
    instructions: 'Đứng trước bục, hơi khuỵu gối lấy đà, bật nhảy hai chân lên bục rồi bước xuống nhẹ nhàng.',
    lottieAsset: `${CLOUD_BASE}/v1781322134/exercises/lottie/box-jump.lottie`,
  },
  {
    name: 'Burpee bật nhảy',
    muscleGroup: 'Chân',
    difficultyLevel: 'ADVANCED',
    metValue: 8.0,
    description: 'Bài tập cường độ cao kết hợp chống đẩy, gập người và bật nhảy, đốt nhiều calo trong thời gian ngắn.',
    instructions: 'Từ tư thế đứng, cúi người chống tay xuống sàn, đẩy hai chân ra sau thành plank, co chân về và bật nhảy lên cao.',
    lottieAsset: `${CLOUD_BASE}/v1781322135/exercises/lottie/burpee-and-jump.lottie`,
  },
  {
    name: 'Tư thế rắn hổ mang',
    muscleGroup: 'Lưng',
    difficultyLevel: 'BEGINNER',
    metValue: 2.5,
    description: 'Bài tập kéo giãn và tăng cường cơ lưng dưới, cải thiện độ linh hoạt cột sống.',
    instructions: 'Nằm sấp, hai tay chống xuống sàn dưới vai, từ từ duỗi thẳng tay nâng ngực lên cao, giữ vài giây rồi hạ xuống.',
    lottieAsset: `${CLOUD_BASE}/v1781322136/exercises/lottie/cobras.lottie`,
  },
  {
    name: 'Cử giật và đẩy tạ đòn hai tay',
    muscleGroup: 'Vai',
    difficultyLevel: 'ADVANCED',
    metValue: 6.0,
    description: 'Bài tập tạ đòn kết hợp kéo tạ từ sàn lên vai và đẩy thẳng qua đầu, phát triển sức mạnh vai và toàn thân.',
    instructions: 'Đứng giữ tạ đòn trước đùi, kéo bật người đưa tạ lên ngang vai, sau đó đẩy thẳng tạ qua đầu rồi hạ xuống vị trí ban đầu.',
    lottieAsset: `${CLOUD_BASE}/v1781322137/exercises/lottie/double-arm-clean-and-press-barbell.lottie`,
  },
  {
    name: 'Ép đùi ếch',
    muscleGroup: 'Đùi trong',
    difficultyLevel: 'BEGINNER',
    metValue: 3.0,
    description: 'Bài tập nằm ép và mở đùi theo tư thế chân ếch, tác động trực tiếp vào cơ đùi trong.',
    instructions: 'Nằm ngửa, co gối mở rộng hai bên như tư thế ếch, ép đùi vào sát người rồi mở ra lặp lại.',
    lottieAsset: `${CLOUD_BASE}/v1781322138/exercises/lottie/frog-press.lottie`,
  },
  {
    name: 'Sâu đo',
    muscleGroup: 'Gân kheo',
    difficultyLevel: 'INTERMEDIATE',
    metValue: 4.5,
    description: 'Bài tập khởi động toàn thân, kéo giãn gân kheo và tăng cường cơ bụng, vai.',
    instructions: 'Đứng thẳng, gập người đặt tay xuống sàn, bước tay ra trước thành plank, sau đó bước chân tiến về tay và trở lại tư thế đứng.',
    lottieAsset: `${CLOUD_BASE}/v1781322139/exercises/lottie/inchworm.lottie`,
  },
  {
    name: 'Nhảy tay chân',
    muscleGroup: 'Chân',
    difficultyLevel: 'BEGINNER',
    metValue: 6.0,
    description: 'Bài tập cardio cơ bản, bật nhảy mở khép tay chân, làm nóng cơ thể và tăng nhịp tim.',
    instructions: 'Đứng thẳng, bật nhảy đồng thời mở hai chân sang ngang và đưa hai tay lên cao, sau đó nhảy về tư thế ban đầu.',
    lottieAsset: `${CLOUD_BASE}/v1781322140/exercises/lottie/jumping-jack.lottie`,
  },
  {
    name: 'Squat bật nhảy',
    muscleGroup: 'Đùi',
    difficultyLevel: 'INTERMEDIATE',
    metValue: 7.0,
    description: 'Bài tập squat kết hợp bật nhảy, tăng sức mạnh và sức bền cho đùi và mông.',
    instructions: 'Hạ người xuống tư thế squat, sau đó bật nhảy thẳng lên cao, đáp đất nhẹ nhàng và trở lại squat.',
    lottieAsset: `${CLOUD_BASE}/v1781322141/exercises/lottie/jumping-squat.lottie`,
  },
  {
    name: 'Lunge bước trước',
    muscleGroup: 'Đùi',
    difficultyLevel: 'BEGINNER',
    metValue: 4.0,
    description: 'Bài tập bước chân về trước và hạ thấp người, tác động mạnh vào cơ đùi và mông.',
    instructions: 'Bước một chân về trước, hạ người xuống cho đến khi cả hai gối gập 90 độ, rồi đẩy người trở lại tư thế đứng.',
    lottieAsset: `${CLOUD_BASE}/v1781322142/exercises/lottie/lunge.lottie`,
  },
  {
    name: 'Chạm mũi chân ở tư thế chống đẩy',
    muscleGroup: 'Bụng',
    difficultyLevel: 'INTERMEDIATE',
    metValue: 5.0,
    description: 'Bài tập giữ tư thế chống đẩy và luân phiên chạm tay vào mũi chân, tăng cường cơ bụng và vai.',
    instructions: 'Giữ tư thế chống đẩy cao, lần lượt đưa một tay chạm vào mũi chân đối diện rồi trở lại, đổi bên liên tục.',
    lottieAsset: `${CLOUD_BASE}/v1781322142/exercises/lottie/press-up-postion-toe-tap.lottie`,
  },
  {
    name: 'Đấm bốc',
    muscleGroup: 'Vai',
    difficultyLevel: 'BEGINNER',
    metValue: 5.0,
    description: 'Bài tập cardio mô phỏng động tác đấm bốc, tăng nhịp tim và sức mạnh cơ vai, tay.',
    instructions: 'Đứng tư thế tấn, luân phiên đấm thẳng hai tay về trước với tốc độ nhanh, kết hợp xoay hông.',
    lottieAsset: `${CLOUD_BASE}/v1781322143/exercises/lottie/punch.lottie`,
  },
  {
    name: 'Chống đẩy',
    muscleGroup: 'Ngực',
    difficultyLevel: 'BEGINNER',
    metValue: 4.0,
    description: 'Bài tập chống đẩy cơ bản, tăng cường sức mạnh cơ ngực, tay sau và vai.',
    instructions: 'Chống hai tay rộng hơn vai, giữ thân thẳng, hạ người xuống gần sàn rồi đẩy lên trở lại.',
    lottieAsset: `${CLOUD_BASE}/v1781322144/exercises/lottie/push-up.lottie`,
  },
  {
    name: 'Gập bụng ngược',
    muscleGroup: 'Bụng',
    difficultyLevel: 'INTERMEDIATE',
    metValue: 3.5,
    description: 'Bài tập gập bụng ngược, tập trung vào vùng bụng dưới.',
    instructions: 'Nằm ngửa, co gối kéo về phía ngực bằng lực cơ bụng dưới, sau đó hạ chân từ từ về vị trí ban đầu.',
    lottieAsset: `${CLOUD_BASE}/v1781322145/exercises/lottie/reverse-crunches.lottie`,
  },
  {
    name: 'Xoay vòng bụng ngồi',
    muscleGroup: 'Bụng',
    difficultyLevel: 'INTERMEDIATE',
    metValue: 4.0,
    description: 'Bài tập ngồi giữ thăng bằng và xoay vòng chân, tác động mạnh vào cơ bụng.',
    instructions: 'Ngồi trên sàn, ngả người về sau giữ thăng bằng, nâng hai chân lên và xoay tròn theo một hướng, đổi hướng sau mỗi vòng.',
    lottieAsset: `${CLOUD_BASE}/v1781322146/exercises/lottie/seated-abs-circles.lottie`,
  },
  {
    name: 'Giãn cơ vai',
    muscleGroup: 'Vai',
    difficultyLevel: 'BEGINNER',
    metValue: 2.0,
    description: 'Bài tập kéo giãn cơ vai, giúp giảm căng cứng và tăng độ linh hoạt khớp vai.',
    instructions: 'Đưa một tay ngang qua ngực, dùng tay còn lại kéo nhẹ về phía thân, giữ vài giây rồi đổi bên.',
    lottieAsset: `${CLOUD_BASE}/v1781322147/exercises/lottie/shoulder-stretch.lottie`,
  },
  {
    name: 'Bật nhảy đổi chân',
    muscleGroup: 'Đùi',
    difficultyLevel: 'ADVANCED',
    metValue: 8.0,
    description: 'Bài tập bật nhảy đổi chân ở tư thế lunge, tăng sức mạnh và sức bền cho đùi.',
    instructions: 'Đứng ở tư thế lunge, bật nhảy lên cao và đổi vị trí hai chân ngay trên không, đáp đất nhẹ nhàng và lặp lại.',
    lottieAsset: `${CLOUD_BASE}/v1781322148/exercises/lottie/split-jump.lottie`,
  },
  {
    name: 'Squat đá chân',
    muscleGroup: 'Đùi',
    difficultyLevel: 'INTERMEDIATE',
    metValue: 6.0,
    description: 'Bài tập kết hợp squat và đá chân ngang, tăng sức mạnh đùi và sự linh hoạt hông.',
    instructions: 'Hạ người xuống tư thế squat, đứng lên và đá một chân sang ngang, trở lại squat rồi đổi chân.',
    lottieAsset: `${CLOUD_BASE}/v1781322149/exercises/lottie/squat-kick.lottie`,
  },
  {
    name: 'Squat vươn tay',
    muscleGroup: 'Đùi',
    difficultyLevel: 'BEGINNER',
    metValue: 4.5,
    description: 'Bài tập squat kết hợp vươn tay lên cao, tăng cường cơ đùi và sự dẻo dai toàn thân.',
    instructions: 'Hạ người xuống tư thế squat, hai tay chạm sàn, sau đó đứng lên và vươn thẳng hai tay lên cao.',
    lottieAsset: `${CLOUD_BASE}/v1781322150/exercises/lottie/squat-reach.lottie`,
  },
  {
    name: 'Chống đẩy tay lệch',
    muscleGroup: 'Ngực',
    difficultyLevel: 'INTERMEDIATE',
    metValue: 5.0,
    description: 'Bài tập chống đẩy với hai tay đặt lệch nhau, tăng cường cơ ngực và tay không đều.',
    instructions: 'Chống hai tay xuống sàn với một tay đặt cao hơn tay kia, hạ người xuống rồi đẩy lên, đổi vị trí tay sau mỗi set.',
    lottieAsset: `${CLOUD_BASE}/v1781322151/exercises/lottie/staggered-push-up.lottie`,
  },
  {
    name: 'Bước lên ghế',
    muscleGroup: 'Đùi',
    difficultyLevel: 'BEGINNER',
    metValue: 5.0,
    description: 'Bài tập bước một chân lên ghế hoặc bậc cao, tăng cường cơ đùi và mông.',
    instructions: 'Đặt một chân lên ghế, dồn lực đẩy người lên cho đến khi chân thẳng, sau đó bước xuống và đổi chân.',
    lottieAsset: `${CLOUD_BASE}/v1781322152/exercises/lottie/step-up-on-chair.lottie`,
  },
  {
    name: 'Mở hông khi đi bộ',
    muscleGroup: 'Hông',
    difficultyLevel: 'BEGINNER',
    metValue: 3.5,
    description: 'Bài tập khởi động mở khớp hông kết hợp bước đi, tăng độ linh hoạt vùng hông.',
    instructions: 'Bước một chân lên trước, nâng gối chân sau xoay mở ra ngoài rồi hạ xuống, tiếp tục bước và đổi chân.',
    lottieAsset: `${CLOUD_BASE}/v1781322153/exercises/lottie/walking-hip-opener.lottie`,
  },
  {
    name: 'Chống đẩy tay rộng',
    muscleGroup: 'Ngực',
    difficultyLevel: 'BEGINNER',
    metValue: 4.0,
    description: 'Bài tập chống đẩy với hai tay đặt rộng hơn vai, tập trung phát triển cơ ngực.',
    instructions: 'Chống hai tay rộng hơn vai, hạ người xuống gần sàn rồi đẩy lên, giữ thân thẳng trong suốt động tác.',
    lottieAsset: `${CLOUD_BASE}/v1781322154/exercises/lottie/wide-arm-push-up.lottie`,
  },
];

async function main() {
  const loginRes = await fetch(`${BASE_URL}/admin/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'admin@gmail.com', password: 'admin123' }),
  });
  if (!loginRes.ok) {
    throw new Error(`Login failed: ${loginRes.status} ${await loginRes.text()}`);
  }
  const loginBody = await loginRes.json();
  const access_token = loginBody.data?.access_token ?? loginBody.access_token;
  console.log('Logged in.');

  for (const ex of exercises) {
    const body = {
      ...ex,
      exerciseType: 'GYM',
      category: '',
    };
    const res = await fetch(`${BASE_URL}/admin/exercises`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${access_token}`,
      },
      body: JSON.stringify(body),
    });
    if (res.ok) {
      const data = await res.json();
      const id = data.data?.id ?? data.id;
      console.log(`OK   ${ex.name} -> id=${id}`);
    } else {
      console.log(`FAIL ${ex.name} -> ${res.status} ${await res.text()}`);
    }
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
