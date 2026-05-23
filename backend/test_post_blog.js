const API_URL = 'http://localhost:3005';

const jwt = require('jsonwebtoken');

async function testPostBlog() {
  console.log('1. Tạo Token trực tiếp...');
  // ID từ database: 2ec2f8e1-d784-463c-a320-459067a89a7d
  const userId = '2ec2f8e1-d784-463c-a320-459067a89a7d';
  const token = jwt.sign({ sub: userId, email: 'test@example.com', role: 'user' }, 'khiemhehe', { expiresIn: '1h' });
  console.log('✅ Tạo Token thành công, Token:', token.substring(0, 20) + '...');


  console.log('\n2. Thực hiện gọi POST /user/blogs...');
  const blogPayload = {
    title: 'Bài viết test từ Script',
    status: 'approved',
    tags: ['Tập luyện', 'Sức khỏe'],
    thumbnailUrl: 'https://example.com/thumbnail.jpg',
    blocks: [
      {
        order: 1,
        type: 'text',
        text_content: 'Đây là đoạn text đầu tiên của bài blog test.'
      },
      {
        order: 2,
        type: 'image',
        image_url: 'https://example.com/image1.jpg'
      }
    ]
  };

  try {
    const postRes = await fetch(`${API_URL}/user/blogs`, {
      method: 'POST',
      headers: { 
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify(blogPayload)
    });
    
    const postData = await postRes.json();
    if (postRes.ok) {
      console.log('✅ Đăng bài thành công!');
      console.log('Kết quả trả về:', JSON.stringify(postData, null, 2));
    } else {
      console.error('❌ Đăng bài thất bại:', postData);
    }
  } catch (error) {
    console.error('Lỗi khi gọi API post blog:', error);
  }
}

testPostBlog();
