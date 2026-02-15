import { http, HttpResponse } from 'msw';

// Test data
export const mockUser = {
  id: 1,
  email: 'test@example.com',
  username: 'testuser',
  role: 'Customer',
  mobile: '1234567890',
  address: '123 Test St',
  carPlateNo: 'ABC123',
  credit: 10.00
};

export const mockAdminUser = {
  id: 2,
  email: 'admin@example.com',
  username: 'admin',
  role: 'Admin',
  mobile: '0987654321',
  address: 'Admin Address',
  carPlateNo: 'ADMIN001',
  credit: 100.00
};

export const mockParkingSpots = [
  { parkingId: 'A-001', availability: 'available' },
  { parkingId: 'A-002', availability: 'available' },
  { parkingId: 'A-003', availability: 'booked' },
  { parkingId: 'B-001', availability: 'available' },
  { parkingId: 'B-002', availability: 'unknown' },
];

export const mockBookings = [
  {
    id: 1,
    userId: 1,
    userName: 'testuser',
    userContact: '1234567890',
    carPlate: 'ABC123',
    parkingSpot: 'A-003',
    date: '2024-02-14T10:00:00',
    status: 'Active',
    credit: 6
  },
  {
    id: 2,
    userId: 1,
    userName: 'testuser',
    userContact: '1234567890',
    carPlate: 'ABC123',
    parkingSpot: 'A-001',
    date: '2024-02-13T10:00:00',
    status: 'Completed',
    credit: 6
  }
];

export const handlers = [
  // Config endpoint
  http.get('/api/config', () => {
    return HttpResponse.json({
      demoMode: true,
      sessionTimeout: 0
    });
  }),

  // Login
  http.post('/api/users/login', async ({ request }) => {
    const body = await request.json();
    
    if (body.email === 'test@example.com' && body.password === 'Password@123') {
      return HttpResponse.json({
        token: 'mock-jwt-token',
        email: 'test@example.com',
        username: 'testuser',
        role: 'Customer'
      });
    }
    
    if (body.email === 'admin@example.com' && body.password === 'Admin@123') {
      return HttpResponse.json({
        token: 'mock-admin-jwt-token',
        email: 'admin@example.com',
        username: 'admin',
        role: 'Admin'
      });
    }
    
    return HttpResponse.json(
      { message: 'Invalid email or password' },
      { status: 401 }
    );
  }),

  // Register
  http.post('/api/users/register', async ({ request }) => {
    const body = await request.json();
    
    if (body.email === 'existing@example.com') {
      return HttpResponse.json(
        { message: 'Email already exists' },
        { status: 400 }
      );
    }
    
    return HttpResponse.json({
      id: 3,
      email: body.email,
      username: body.username,
      role: 'Customer',
      credit: 0
    });
  }),

  // Get current user
  http.get('/api/users/me', () => {
    return HttpResponse.json(mockUser);
  }),

  // Update user
  http.put('/api/users/:id', async ({ request, params }) => {
    const body = await request.json();
    return HttpResponse.json({
      ...mockUser,
      ...body
    });
  }),

  // Parking endpoints
  http.get('/api/parking/available/count', () => {
    const available = mockParkingSpots.filter(p => p.availability === 'available').length;
    return HttpResponse.json({ available });
  }),

  http.get('/api/parking/available', () => {
    return HttpResponse.json(
      mockParkingSpots.filter(p => p.availability === 'available')
    );
  }),

  http.get('/api/parking', () => {
    return HttpResponse.json(mockParkingSpots);
  }),

  // Bookings
  http.get('/api/bookings/my-active', () => {
    return HttpResponse.json(
      mockBookings.filter(b => b.status === 'Active')
    );
  }),

  http.get('/api/bookings/user/:userId', () => {
    return HttpResponse.json(mockBookings);
  }),

  http.get('/api/bookings', () => {
    return HttpResponse.json(mockBookings);
  }),

  http.post('/api/bookings', async ({ request }) => {
    const body = await request.json();
    return HttpResponse.json({
      id: 3,
      userId: body.userId,
      userName: 'testuser',
      userContact: '1234567890',
      carPlate: body.carPlate,
      parkingSpot: body.parkingId,
      date: new Date().toISOString(),
      status: 'Active',
      credit: 6
    });
  }),

  http.post('/api/bookings/:id/cancel', ({ params }) => {
    return HttpResponse.json({ message: 'Booking cancelled successfully' });
  }),

  http.post('/api/bookings/:id/report-taken', ({ params }) => {
    return HttpResponse.json({
      ...mockBookings[0],
      parkingSpot: 'B-001'
    });
  }),

  // Admin endpoints
  http.get('/api/admin/users', () => {
    return HttpResponse.json([mockUser, mockAdminUser]);
  }),

  http.get('/api/admin/bookings', () => {
    return HttpResponse.json(mockBookings);
  }),

  http.get('/api/admin/parking', () => {
    return HttpResponse.json(mockParkingSpots);
  }),

  http.get('/api/admin/dashboard', () => {
    return HttpResponse.json({
      totalUsers: 2,
      totalBookings: 2,
      activeBookings: 1,
      totalParkingSpots: 5,
      availableSpots: 3
    });
  }),

  http.delete('/api/admin/users/:id', () => {
    return HttpResponse.json({ message: 'User deleted successfully' });
  }),

  http.delete('/api/admin/bookings/:id', () => {
    return HttpResponse.json({ message: 'Booking deleted successfully' });
  }),

  http.post('/api/admin/users/:id/credit', () => {
    return HttpResponse.json({ message: 'Credit added successfully' });
  }),

  http.put('/api/admin/users/:id', async ({ request, params }) => {
    const body = await request.json();
    return HttpResponse.json({
      id: parseInt(params.id),
      ...body
    });
  }),
];
