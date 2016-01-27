package project;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;

import com.mysql.jdbc.Statement;

/**
 * Class for representing a connection to the library
 * 
 * @author Kyle Pierson
 *
 */
public class LibraryConnection {
	private Connection conn = null;
	private PreparedStatement stmt = null;

	public LibraryConnection(String url, String username, String password) throws ConnectionException {
		try {
			// Set up the connection
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection(url, username, password);
			conn.setAutoCommit(false);
		} catch (Exception e) {
			throw new ConnectionException("A connection could not be established: " + e.getMessage());
		}
	}

	/**
	 * Register a new user
	 * 
	 * @param info
	 * @return The newly issued card id, or -1 if failed
	 * @throws ConnectionException
	 */
	public int register(String[] info) throws ConnectionException {
		ResultSet rs = null;
		try {
			// Insert the user
			String query = "INSERT INTO users (username,fullname,address,city,state,zip,phone,email)"
					+ " VALUES (?,?,?,?,?,?,?,?)";
			stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

			stmt.setString(1, info[0]);
			stmt.setString(2, info[1]);
			stmt.setString(3, info[2]);
			stmt.setString(4, info[3]);
			stmt.setString(5, info[4]);
			stmt.setString(6, info[5]);
			stmt.setString(7, info[6]);
			stmt.setString(8, info[7]);

			stmt.executeUpdate();

			// Get the card id
			rs = stmt.getGeneratedKeys();
			conn.commit();

			if (rs.next())
				return rs.getInt(1);
			else
				return -1;
		} catch (Exception e) {
			try {
				conn.rollback();
			} catch (SQLException exc) {
			}
			throw new ConnectionException("User registration failed: " + e.getMessage());
		} finally {
			try {
				rs.close();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * Ensures the user exists in the databases, returning his or her card id
	 * 
	 * @param username
	 * @return The card id of the user on success, -1 on failure
	 * @throws ConnectionException
	 */
	public int login(String username) throws ConnectionException {
		ResultSet rs = null;
		try {
			// Find the user
			String query = "SELECT card_id FROM users WHERE username = ?";
			stmt = conn.prepareStatement(query);
			stmt.setString(1, username);

			// Get the card id and return it to the caller
			rs = stmt.executeQuery();
			if (rs.next()) {
				int card_id = rs.getInt(1);
				System.out.println("Logged in, your card number is " + card_id);
				return card_id;
			} else
				return -1;
		} catch (Exception e) {
			throw new ConnectionException("Login failed: " + e.getMessage());
		} finally {
			try {
				rs.close();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * Checks a book out from the library
	 * 
	 * @param card_id
	 * @param book
	 * @throws ConnectionException
	 */
	public void checkout(int card_id, String book) throws ConnectionException {
		ResultSet rs = null;
		ResultSet rs2 = null;
		try {
			// Find the book in the library
			String query = "SELECT books.isbn, book_id FROM books NATURAL JOIN book_copies "
					+ "WHERE (books.isbn = ? OR title = ?) AND in_stock = 1";
			stmt = conn.prepareStatement(query);

			stmt.setString(1, book);
			stmt.setString(2, book);
			rs = stmt.executeQuery();

			if (!rs.next()) {
				System.out.println("That book is not in stock");
				return;
			}

			String isbn = rs.getString(1);
			int book_id = rs.getInt(2);

			// Find out if others are waiting for this book
			query = "SELECT card_id FROM requests WHERE isbn = ? AND fulfilled = 0" + " ORDER BY date_requested ASC";
			stmt = conn.prepareStatement(query);

			stmt.setString(1, isbn);
			rs2 = stmt.executeQuery();

			if (rs2.next() && rs2.getInt(1) != card_id) {
				System.out.println("You are not first on the wait list for that book");
				return;
			}

			// If this user is waiting, the request is fulfilled
			query = "UPDATE requests SET fulfilled = 1 WHERE isbn = ? AND card_id = ?";
			stmt = conn.prepareStatement(query);
			stmt.setString(1, isbn);
			stmt.setInt(2, card_id);

			stmt.executeUpdate();

			// This book is no longer in stock
			query = "UPDATE book_copies SET in_stock = 0 WHERE book_id = ?";
			stmt = conn.prepareStatement(query);

			stmt.setInt(1, book_id);
			stmt.executeUpdate();

			// Finally, add this to the checkout table
			query = "INSERT INTO checkouts (book_id, card_id, check_date, due_date)"
					+ "VALUES (?, ?, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 30 DAY))";
			stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

			stmt.setInt(1, book_id);
			stmt.setInt(2, card_id);

			stmt.executeUpdate();

			Calendar c = Calendar.getInstance();
			c.setTime(new Date());
			c.add(Calendar.DATE, 30);

			SimpleDateFormat date_format = new SimpleDateFormat("MM/dd/yyyy");
			String due_date = date_format.format(c.getTime());

			System.out.println("The book id of your checkout is " + book_id);
			System.out.println("Your book is due on " + due_date);

			conn.commit();
		} catch (Exception e) {
			try {
				conn.rollback();
			} catch (SQLException exc) {
			}
			throw new ConnectionException("Checkout failed: " + e.getMessage());
		} finally {
			try {
				rs.close();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * Returns a book from the specified user If the user has more than one copy
	 * of the same book, returns the one with the earliest due date
	 * 
	 * @param card_id
	 * @param book
	 * @throws ConnectionException
	 */
	public void return_book(int card_id, String book) throws ConnectionException {
		ResultSet rs = null;
		ResultSet rs2 = null;
		try {
			// Find the book that the user is returning
			String query = "SELECT check_id, checkouts.book_id, books.isbn FROM"
					+ " (checkouts NATURAL JOIN book_copies) NATURAL JOIN books"
					+ " WHERE card_id = ? AND (books.isbn = ? OR title = ?) AND check_id NOT IN"
					+ " (SELECT check_id FROM returned) AND check_id NOT IN" + " (SELECT check_id FROM lost)"
					+ " ORDER BY check_date ASC";
			stmt = conn.prepareStatement(query);

			stmt.setInt(1, card_id);
			stmt.setString(2, book);
			stmt.setString(3, book);
			rs = stmt.executeQuery();

			if (!rs.next()) {
				System.out.println("You do not have a book checked-out with that isbn/title");
				return;
			}

			int check_id = rs.getInt(1);
			int book_id = rs.getInt(2);
			String isbn = rs.getString(3);

			// The book is not back in stock
			query = "UPDATE book_copies SET in_stock = 1 WHERE book_id = ?";
			stmt = conn.prepareStatement(query);
			stmt.setInt(1, book_id);

			stmt.executeUpdate();

			// Enter a row into the returned table
			query = "INSERT INTO returned (check_id, date_returned) VALUES (?, CURDATE())";
			stmt = conn.prepareStatement(query);
			stmt.setInt(1, check_id);

			stmt.executeUpdate();

			System.out.println("Your book has been returned");
			System.out.println("");

			// Get the names of all those waiting for the recently returned book
			query = "SELECT fullname, username, users.card_id FROM requests NATURAL JOIN users WHERE isbn = ? AND fulfilled = 0 ORDER BY date_requested ASC";
			stmt = conn.prepareStatement(query);
			stmt.setString(1, isbn);
			rs2 = stmt.executeQuery();

			System.out.println("The following users are on the wait list for the returned book");

			int i = 1;
			while (rs2.next()) {
				System.out.println(i + ". " + rs2.getString(1) + " (username: " + rs2.getString(2) + " card id: "
						+ rs2.getInt(3) + ")");
				i++;
			}
			System.out.println("");
			conn.commit();
		} catch (Exception e) {
			try {
				conn.rollback();
			} catch (SQLException exc) {
			}
			throw new ConnectionException("Return failed: " + e.getMessage());
		} finally {
			try {
				rs.close();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * Submits a request from the specified user for the specified book
	 * 
	 * @param card_id
	 * @param book
	 * @throws ConnectionException
	 */
	public void request(int card_id, String book) throws ConnectionException {
		ResultSet rs = null;
		ResultSet rs2 = null;
		try {
			// Make sure the book exists
			String query = "SELECT isbn FROM books WHERE isbn = ? OR title = ?";
			stmt = conn.prepareStatement(query);
			stmt.setString(1, book);
			stmt.setString(2, book);
			rs = stmt.executeQuery();

			// Doesn't exist
			if (!rs.next()) {
				System.out.println("That book does not exist in our library");
				return;
			}

			String isbn = rs.getString(1);

			// Get the number of people in front of this person for the same
			// book
			query = "SELECT COUNT(*) FROM requests WHERE isbn = ? AND fulfilled = 0";
			stmt = conn.prepareStatement(query);
			stmt.setString(1, isbn);
			rs2 = stmt.executeQuery();

			int count = 1;
			if (rs2.next())
				count += rs2.getInt(1);

			query = "INSERT INTO requests (isbn, card_id, date_requested, fulfilled)" + " VALUES (?, ?, NOW(), 0)";
			stmt = conn.prepareStatement(query);
			stmt.setString(1, isbn);
			stmt.setInt(2, card_id);

			stmt.executeUpdate();

			System.out.println("Your request was submitted, you are number " + count + " on the waiting list");
			conn.commit();
		} catch (Exception e) {
			try {
				conn.rollback();
			} catch (SQLException exc) {
			}
			throw new ConnectionException("Request failed: " + e.getMessage());
		} finally {
			try {
				rs.close();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * Submits a review from the user for the book
	 * 
	 * @param card_id
	 * @param book
	 * @param score
	 * @param review
	 * @throws ConnectionException
	 */
	public void review(int card_id, String book, int score, String review) throws ConnectionException {
		ResultSet rs = null;
		try {
			// Make sure it exists
			String query = "SELECT isbn FROM books WHERE isbn = ? OR title = ?";
			stmt = conn.prepareStatement(query);
			stmt.setString(1, book);
			stmt.setString(2, book);
			rs = stmt.executeQuery();

			if (!rs.next()) {
				System.out.println("That book does not exist in our library");
				return;
			}

			String isbn = rs.getString(1);

			query = "INSERT INTO reviews (card_id, isbn, score, review) VALUES (?, ?, ?, ?)";
			stmt = conn.prepareStatement(query);
			stmt.setInt(1, card_id);
			stmt.setString(2, isbn);
			stmt.setInt(3, score);
			stmt.setString(4, review);
			stmt.executeUpdate();

			System.out.println("Your review was submitted");
			conn.commit();
		} catch (Exception e) {
			try {
				conn.rollback();
			} catch (SQLException exc) {
			}
			throw new ConnectionException("Review failed: " + e.getMessage());
		} finally {
			try {
				rs.close();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * Reports a book as lost from the specified user
	 * 
	 * @param card_id
	 * @param book
	 * @throws ConnectionException
	 */
	public void lost(int card_id, String book) throws ConnectionException {
		ResultSet rs = null;
		try {
			// Find the book reported as lost
			String query = "SELECT check_id, checkouts.book_id FROM"
					+ " (checkouts NATURAL JOIN book_copies) NATURAL JOIN books"
					+ " WHERE card_id = ? AND (books.isbn = ? OR title = ?)"
					+ " AND check_id NOT IN (SELECT check_id FROM returned)"
					+ " AND check_id NOT IN (SELECT check_id FROM lost)" + " ORDER BY check_date ASC";
			stmt = conn.prepareStatement(query);

			stmt.setInt(1, card_id);
			stmt.setString(2, book);
			stmt.setString(3, book);
			rs = stmt.executeQuery();

			if (!rs.next()) {
				System.out.println("You do not have a book currently checked out with that isbn/title");
				return;
			}

			int check_id = rs.getInt(1);

			// Report as lost
			query = "INSERT INTO lost (check_id, date_lost) VALUES (?, CURDATE())";
			stmt = conn.prepareStatement(query);
			stmt.setInt(1, check_id);

			stmt.executeUpdate();

			System.out.println("Your book has been recorded as lost");
			conn.commit();
		} catch (Exception e) {
			try {
				conn.rollback();
			} catch (SQLException exc) {
			}
			throw new ConnectionException("Return failed: " + e.getMessage());
		} finally {
			try {
				rs.close();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * Adds the book info to the library
	 * 
	 * @param info
	 * @throws ConnectionException
	 */
	public void add_book(String[] info, String[] authors) throws ConnectionException {
		try {
			String query = "INSERT INTO books (isbn,title,publisher,year,subject,summary,format)"
					+ " VALUES (?,?,?,?,?,?,?)";
			stmt = conn.prepareStatement(query);

			stmt.setString(1, info[0]);
			stmt.setString(2, info[1]);
			stmt.setString(3, info[2]);
			stmt.setString(4, info[3]);
			stmt.setString(5, info[4]);
			stmt.setString(6, info[5]);
			stmt.setString(7, info[6]);

			stmt.executeUpdate();

			// Add author to the author list
			for (String s : authors) {
				query = "INSERT INTO authors (a_name, isbn)" + " VALUES (?,?)";
				stmt = conn.prepareStatement(query);
				stmt.setString(1, s);
				stmt.setString(2, info[0]);

				stmt.executeUpdate();
			}
			conn.commit();
		} catch (Exception e) {
			try {
				conn.rollback();
			} catch (SQLException exc) {
			}
			throw new ConnectionException(
					"Book addition failed: " + e.getMessage() + "\r\nPerhaps that book already existed");
		}
	}

	/**
	 * Adds (num) copies of the book to the library
	 * 
	 * @param info
	 * @param num
	 * @throws ConnectionException
	 */
	public void add_copies(String[] info, int num) throws ConnectionException {
		try {
			for (int i = 0; i < num; i++) {
				String query = "INSERT INTO book_copies (isbn,location,in_stock)" + " VALUES (?,?,1)";
				stmt = conn.prepareStatement(query);

				stmt.setString(1, info[0]);
				stmt.setString(2, info[1]);

				stmt.executeUpdate();
			}

			conn.commit();
		} catch (Exception e) {
			try {
				conn.rollback();
			} catch (SQLException exc) {
			}
			throw new ConnectionException("Book copy addition failed: " + e.getMessage());
		}
	}

	/**
	 * Prints the user record for the specified user
	 * 
	 * @param card_id
	 * @throws ConnectionException
	 */
	public void userRecord(int card_id) throws ConnectionException {
		ResultSet rs = null;
		ResultSet rs2 = null;
		ResultSet rs3 = null;
		ResultSet rs4 = null;
		ResultSet rs5 = null;
		ResultSet rs6 = null;
		try {
			// Get the user info based on card id
			String query = "SELECT * FROM users WHERE card_id = ?";
			stmt = conn.prepareStatement(query);
			stmt.setInt(1, card_id);

			rs = stmt.executeQuery();
			if (rs.next()) {
				new Columns().addLine("Name", "Username", "Street", "City", "State", "Zip", "Phone", "Email")
						.addLine(rs.getString(3), rs.getString(2), rs.getString(4), rs.getString(5), rs.getString(6),
								rs.getString(7), rs.getString(8), rs.getString(9))
						.print();
			} else {
				System.out.println("That user does not exist");
				return;
			}

			// Get the currently checked out books
			query = "SELECT books.isbn, title, check_date, due_date FROM (checkouts NATURAL JOIN book_copies) NATURAL JOIN books"
					+ " WHERE card_id = ? AND check_id NOT IN" + " (SELECT check_id FROM returned) AND check_id NOT IN"
					+ " (SELECT check_id FROM lost)";
			stmt = conn.prepareStatement(query);
			stmt.setInt(1, card_id);

			rs2 = stmt.executeQuery();
			System.out.println("Books currently checked-out by this user");
			Columns c = new Columns();
			c.addLine("ISBN", "Title", "Checkout-Date", "Due-Date");
			while (rs2.next()) {
				c.addLine(rs2.getString(1), rs2.getString(2), rs2.getString(3), rs2.getString(4));
			}
			c.print();

			// Get returned books
			query = "SELECT books.isbn, title, check_date, date_returned FROM"
					+ " ((checkouts NATURAL JOIN book_copies) NATURAL JOIN books)"
					+ " NATURAl JOIN returned WHERE card_id = ?";
			stmt = conn.prepareStatement(query);
			stmt.setInt(1, card_id);

			rs3 = stmt.executeQuery();
			System.out.println("Books returned out by this user");
			Columns c1 = new Columns();
			c1.addLine("ISBN", "Title", "Checkout-Date", "Return-Date");
			while (rs3.next()) {
				c1.addLine(rs3.getString(1), rs3.getString(2), rs3.getString(3), rs3.getString(4));
			}
			c1.print();

			// Get lost books
			query = "SELECT books.isbn, title, check_date, date_lost FROM"
					+ " ((checkouts NATURAL JOIN book_copies) NATURAL JOIN books)"
					+ " NATURAL JOIN lost WHERE card_id = ?";
			stmt = conn.prepareStatement(query);
			stmt.setInt(1, card_id);

			rs4 = stmt.executeQuery();
			System.out.println("Books lost out by this user");
			Columns c2 = new Columns();
			c2.addLine("ISBN", "Title", "Checkout-Date", "Date-Lost");
			while (rs4.next()) {
				c2.addLine(rs4.getString(1), rs4.getString(2), rs4.getString(3), rs4.getString(4));
			}
			c2.print();

			// Get book requests from this user
			query = "SELECT books.isbn, title, date_requested FROM books NATURAL JOIN requests"
					+ " WHERE card_id = ? AND fulfilled = 0";
			stmt = conn.prepareStatement(query);
			stmt.setInt(1, card_id);

			rs5 = stmt.executeQuery();
			System.out.println("Pending book requests from this user");
			Columns c3 = new Columns();
			c3.addLine("ISBN", "Title", "Request Date");
			while (rs5.next()) {
				c3.addLine(rs5.getString(1), rs5.getString(2), rs5.getString(3));
			}
			c3.print();

			// Get reviews submitted by the user
			query = "SELECT books.isbn, title, score, review FROM books NATURAL JOIN reviews WHERE card_id = ?";
			stmt = conn.prepareStatement(query);
			stmt.setInt(1, card_id);

			rs6 = stmt.executeQuery();
			System.out.println("Book reviews submitted by this user");
			Columns c4 = new Columns();
			c4.addLine("ISBN", "Title", "Score", "Review");
			while (rs6.next()) {
				c4.addLine(rs6.getString(1), rs6.getString(2), rs6.getString(3), rs6.getString(4));
			}
			c4.print();
		} catch (Exception e) {
			throw new ConnectionException("User info query failed: " + e.getMessage());
		} finally {
			try {
				rs.close();
			} catch (Exception e) {
			}
			try {
				rs2.close();
			} catch (Exception e) {
			}
			try {
				rs3.close();
			} catch (Exception e) {
			}
			try {
				rs4.close();
			} catch (Exception e) {
			}
			try {
				rs5.close();
			} catch (Exception e) {
			}
			try {
				rs6.close();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * When given a date, prints a list of all books that are late
	 * 
	 * @param date
	 * @throws ConnectionException
	 */
	public void getLate(Date date) throws ConnectionException {
		ResultSet rs = null;
		try {
			String query = "SELECT books.isbn, title, due_date, fullname, phone, email FROM"
					+ " ((checkouts NATURAL JOIN book_copies) NATURAL JOIN books) NATURAL JOIN users"
					+ " WHERE check_id NOT IN" + " (SELECT check_id FROM returned) AND check_id NOT IN"
					+ " (SELECT check_id FROM lost)";
			stmt = conn.prepareStatement(query);
			rs = stmt.executeQuery();

			System.out.println("Listing all books late after " + date.toString());
			Columns c = new Columns();
			c.addLine("ISBN", "Title", "Due Date", "User", "Phone", "Email");
			while (rs.next()) {
				if (date.after(rs.getDate(3)))
					c.addLine(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
							rs.getString(6));
			}
			c.print();
		} catch (Exception e) {
			throw new ConnectionException("Late book query failed: " + e.getMessage());
		} finally {
			try {
				rs.close();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * Searches the library for specified keywords option 0 = all books, 1 = all
	 * available books sort 0 = sort by year, 1 = sort by average score, 2 =
	 * sort by popularity
	 * 
	 * @param author
	 * @param publisher
	 * @param title
	 * @param subject
	 * @param option
	 * @param sort
	 * @throws ConnectionException
	 */
	public void browseBooks(String author, String publisher, String title, String subject, int option, int sort)
			throws ConnectionException {
		ResultSet rs = null;
		ResultSet rs2 = null;
		try {
			// Start query
			String query = "SELECT books.isbn, title, publisher, subject";

			if (sort == 0)
				query += ", year";
			else if (sort == 1)
				query += ", AVG(CASE WHEN score IS NULL THEN 0 ELSE score END) as scores";
			else if (sort == 2)
				query += ", SUM(CASE WHEN check_id IS NULL THEN 0 ELSE 1 END) as count";

			query += ", SUM(CASE WHEN in_stock = 0 THEN 0 ELSE 1 END) as stocked";

			query += " FROM (books";

			if (option == 1)
				query += " NATURAL JOIN book_copies)";
			else
				query += " LEFT JOIN book_copies ON books.isbn = book_copies.isbn)";

			if (sort == 1)
				query += " LEFT JOIN reviews ON books.isbn = reviews.isbn";
			else if (sort == 2)
				query += " LEFT JOIN checkouts ON book_copies.book_id = checkouts.book_id";

			query += " WHERE publisher LIKE '%" + publisher + "%' AND title LIKE '%" + title + "%' AND subject LIKE '%"
					+ subject + "%' AND books.isbn IN" + " (SELECT isbn FROM authors WHERE a_name LIKE '%" + author
					+ "%')";

			query += " GROUP BY books.isbn";

			if (sort == 0)
				query += " ORDER BY year DESC";
			else if (sort == 1)
				query += " ORDER BY scores DESC";
			else
				query += " ORDER BY count DESC";

			stmt = conn.prepareStatement(query);
			rs = stmt.executeQuery();

			String s;
			if (sort == 0)
				s = "Year";
			else if (sort == 1)
				s = "Average Score";
			else
				s = "Number of Checkouts";

			Columns c = new Columns();
			c.addLine("ISBN", "Title", "Author(s)", "Publisher", "Subject", s);

			while (rs.next()) {
				if (option == 1 && rs.getInt(6) == 0)
					continue;

				query = "SELECT a_name FROM authors WHERE isbn = ?";
				stmt = conn.prepareStatement(query);
				stmt.setString(1, rs.getString(1));
				rs2 = stmt.executeQuery();

				String authors = "";
				if (rs2.next())
					authors = rs2.getString(1);

				while (rs2.next())
					authors += ", " + rs2.getString(1);

				c.addLine(rs.getString(1), rs.getString(2), authors, rs.getString(3), rs.getString(4), rs.getString(5));
			}
			c.print();
		} catch (Exception e) {
			throw new ConnectionException("Book browsing failed: " + e.getMessage());
		} finally {
			try {
				rs.close();
			} catch (Exception e) {
			}
			try {
				rs2.close();
			} catch (Exception e) {
			}
		}
	}

	public void book_info(String book) throws ConnectionException {
		ResultSet rs = null;
		ResultSet rs2 = null;
		ResultSet rs3 = null;
		ResultSet rs4 = null;
		ResultSet rs5 = null;
		ResultSet rs6 = null;
		ResultSet rs7 = null;

		try {
			// Find the book
			String query = "SELECT books.isbn, title, publisher, subject, year, summary, format, a_name"
					+ " FROM books NATURAL JOIN authors WHERE books.isbn = ? OR title = ?";
			stmt = conn.prepareStatement(query);
			stmt.setString(1, book);
			stmt.setString(2, book);
			rs = stmt.executeQuery();

			if (!rs.next()) {
				System.out.println("That book does not exist in the library");
				return;
			}

			String isbn = rs.getString(1);
			String title = rs.getString(2);
			String publisher = rs.getString(3);
			String subject = rs.getString(4);
			String year = rs.getString(5);
			String summary = rs.getString(6);
			String format = rs.getString(7);
			String authors = rs.getString(8);
			Columns c = new Columns();
			c.addLine("ISBN", "Title", "Author(s)", "Publisher", "Year", "Subject", "Summary", "Format");
			while (rs.next()) {
				authors += ", " + rs.getString(8);
			}

			c.addLine(isbn, title, authors, publisher, year, subject, summary, format);
			c.print();

			Columns c2 = new Columns();
			c2.addLine("Book ID", "Location");
			Columns c3 = new Columns();
			c3.addLine("Book ID", "User", "Checkout-Date", "Date-Returned", "Date-Lost");

			query = "SELECT book_id, location FROM book_copies WHERE isbn = ? AND in_stock = 1";
			stmt = conn.prepareStatement(query);
			stmt.setString(1, isbn);
			rs2 = stmt.executeQuery();

			while (rs2.next()) {
				c2.addLine("" + rs2.getInt(1), rs2.getString(2));
			}

			query = "SELECT book_id, fullname, check_date, date_returned FROM"
					+ " ((book_copies NATURAL JOIN checkouts) NATURAL JOIN returned)"
					+ " NATURAL JOIN users WHERE isbn = ?";
			stmt = conn.prepareStatement(query);
			stmt.setString(1, isbn);
			rs3 = stmt.executeQuery();

			while (rs3.next()) {
				c3.addLine("" + rs3.getInt(1), rs3.getString(2), rs3.getString(3), rs3.getString(4), "");
			}

			query = "SELECT book_id, fullname, check_date, date_lost FROM"
					+ " ((book_copies NATURAL JOIN checkouts) NATURAL JOIN lost)"
					+ " NATURAL JOIN users WHERE isbn = ?";
			stmt = conn.prepareStatement(query);
			stmt.setString(1, isbn);
			rs4 = stmt.executeQuery();

			while (rs4.next()) {
				c2.addLine("" + rs4.getInt(1), "lost by " + rs4.getString(2));
				c3.addLine("" + rs4.getInt(1), rs4.getString(2), rs4.getString(3), "", rs4.getString(4));
			}

			query = "SELECT book_id, fullname, check_date FROM (book_copies NATURAL JOIN checkouts)"
					+ " NATURAL JOIN users" + " WHERE isbn = ? AND in_stock = 0 AND check_id NOT IN"
					+ " (SELECT check_id FROM lost) AND check_id NOT IN" + " (SELECT check_id FROM returned)";

			stmt = conn.prepareStatement(query);
			stmt.setString(1, isbn);
			rs5 = stmt.executeQuery();

			while (rs5.next()) {
				c2.addLine("" + rs5.getInt(1), "checked-out by " + rs5.getString(2));
				c3.addLine("" + rs5.getInt(1), rs5.getString(2), rs5.getString(3), "", "");
			}

			c2.print();
			c3.print();

			query = "SELECT AVG(score) as score FROM reviews WHERE isbn = ?";
			stmt = conn.prepareStatement(query);
			stmt.setString(1, isbn);
			rs6 = stmt.executeQuery();

			if (rs6.next())
				System.out.println("Average review score: " + rs6.getDouble(1));
			else
				System.out.println("Average review score: 0 (No reviews)");

			query = "SELECT review FROM reviews WHERE isbn = ?";
			stmt = conn.prepareStatement(query);
			stmt.setString(1, isbn);
			rs7 = stmt.executeQuery();

			System.out.println("Reviews of this book");
			int count = 1;
			while (rs7.next()) {
				System.out.println("" + count + ". " + rs7.getString(1));
				count++;
			}
			System.out.println("");
		} catch (Exception e) {
			throw new ConnectionException("Book info failed: " + e.getMessage());
		} finally {
			try {
				rs.close();
			} catch (Exception e) {
			}
			try {
				rs2.close();
			} catch (Exception e) {
			}
			try {
				rs3.close();
			} catch (Exception e) {
			}
			try {
				rs4.close();
			} catch (Exception e) {
			}
			try {
				rs5.close();
			} catch (Exception e) {
			}
			try {
				rs6.close();
			} catch (Exception e) {
			}
			try {
				rs7.close();
			} catch (Exception e) {
			}
		}
	}

	public void checkStats(int val) throws ConnectionException {
		ResultSet rs = null;
		try {
			String query = "SELECT title, books.isbn, COUNT(books.isbn) FROM"
					+ " (checkouts NATURAL JOIN book_copies) NATURAL JOIN books"
					+ " GROUP BY books.isbn ORDER BY COUNT(books.isbn) DESC";
			stmt = conn.prepareStatement(query);
			rs = stmt.executeQuery();

			int i = 0;
			Columns c = new Columns();
			c.addLine("Number", "Title", "ISBN", "Checkouts");
			while (rs.next() && i < val) {
				c.addLine("" + ++i, rs.getString(1), rs.getString(2), "" + rs.getInt(3));
			}
			c.print();
		} catch (Exception e) {
			throw new ConnectionException("Checkout stats failed: " + e.getMessage());
		} finally {
			try {
				rs.close();
			} catch (Exception e) {
			}
		}
	}

	public void reqStats(int val) throws ConnectionException {
		ResultSet rs = null;
		try {
			String query = "SELECT title, books.isbn, COUNT(books.isbn) FROM requests NATURAL JOIN books GROUP BY books.isbn ORDER BY COUNT(books.isbn) DESC";
			stmt = conn.prepareStatement(query);
			rs = stmt.executeQuery();

			int i = 0;
			Columns c = new Columns();
			c.addLine("Number", "Title", "ISBN", "Requests");
			while (rs.next() && i < val) {
				c.addLine("" + ++i, rs.getString(1), rs.getString(2), "" + rs.getInt(3));
			}
			c.print();
		} catch (Exception e) {
			throw new ConnectionException("Requests stats failed: " + e.getMessage());
		} finally {
			try {
				rs.close();
			} catch (Exception e) {
			}
		}
	}

	public void lostStats(int val) throws ConnectionException {
		ResultSet rs = null;
		try {
			String query = "SELECT title, books.isbn, COUNT(books.isbn) FROM ((checkouts NATURAL JOIN lost) NATURAL JOIN book_copies) NATURAL JOIN books GROUP BY books.isbn ORDER BY COUNT(books.isbn) DESC";
			stmt = conn.prepareStatement(query);
			rs = stmt.executeQuery();

			int i = 0;
			Columns c = new Columns();
			c.addLine("Number", "Title", "ISBN", "Books Lost");
			while (rs.next() && i < val) {
				c.addLine("" + ++i, rs.getString(1), rs.getString(2), "" + rs.getInt(3));
			}
			c.print();
		} catch (Exception e) {
			throw new ConnectionException("Lost stats failed: " + e.getMessage());
		} finally {
			try {
				rs.close();
			} catch (Exception e) {
			}
		}
	}

	public void authStats(int val) throws ConnectionException {
		ResultSet rs = null;
		try {
			String query = "SELECT a_name, COUNT(a_name) FROM ((checkouts NATURAL JOIN book_copies)"
					+ " NATURAL JOIN books) INNER JOIN authors ON books.isbn = authors.isbn"
					+ " GROUP BY a_name ORDER BY COUNT(a_name) DESC";
			stmt = conn.prepareStatement(query);
			rs = stmt.executeQuery();

			int i = 0;
			Columns c = new Columns();
			c.addLine("Number", "Author", "Checkouts of books by this author");
			while (rs.next() && i < val) {
				c.addLine("" + ++i, rs.getString(1), "" + rs.getInt(2));
			}
			c.print();
		} catch (Exception e) {
			throw new ConnectionException("Author stats failed: " + e.getMessage());
		} finally {
			try {
				rs.close();
			} catch (Exception e) {
			}
		}
	}

	public void userCheckStats(int val) throws ConnectionException {
		ResultSet rs = null;
		try {
			String query = "SELECT fullname, COUNT(card_id) FROM checkouts NATURAL JOIN users"
					+ " GROUP BY card_id ORDER BY COUNT(card_id) DESC";
			stmt = conn.prepareStatement(query);
			rs = stmt.executeQuery();

			int i = 0;
			Columns c = new Columns();
			c.addLine("Number", "User", "Checkouts");
			while (rs.next() && i < val) {
				c.addLine("" + ++i, rs.getString(1), "" + rs.getInt(2));
			}
			c.print();
		} catch (Exception e) {
			throw new ConnectionException("User checkout stats failed: " + e.getMessage());
		} finally {
			try {
				rs.close();
			} catch (Exception e) {
			}
		}
	}

	public void userRevStats(int val) throws ConnectionException {
		ResultSet rs = null;
		try {
			String query = "SELECT fullname, COUNT(card_id) FROM reviews NATURAL JOIN users"
					+ " GROUP BY card_id ORDER BY COUNT(card_id) DESC";
			stmt = conn.prepareStatement(query);
			rs = stmt.executeQuery();

			int i = 0;
			Columns c = new Columns();
			c.addLine("Number", "User", "Reviews");
			while (rs.next() && i < val) {
				c.addLine("" + ++i, rs.getString(1), "" + rs.getInt(2));
			}
			c.print();
		} catch (Exception e) {
			throw new ConnectionException("Lost stats failed: " + e.getMessage());
		} finally {
			try {
				rs.close();
			} catch (Exception e) {
			}
		}
	}

	public void userLoseStats(int val) throws ConnectionException {
		ResultSet rs = null;
		try {
			String query = "SELECT fullname, COUNT(card_id) FROM (checkouts NATURAL JOIN lost)"
					+ " NATURAL JOIN users GROUP BY card_id ORDER BY COUNT(card_id) DESC";
			stmt = conn.prepareStatement(query);
			rs = stmt.executeQuery();

			int i = 0;
			Columns c = new Columns();
			c.addLine("Number", "User", "Losses");
			while (rs.next() && i < val) {
				c.addLine("" + ++i, rs.getString(1), "" + rs.getInt(2));
			}
			c.print();
		} catch (Exception e) {
			throw new ConnectionException("User lose stats failed: " + e.getMessage());
		} finally {
			try {
				rs.close();
			} catch (Exception e) {
			}
		}
	}

	public void close() {
		try {
			stmt.close();
		} catch (Exception e) {
		}
		try {
			conn.close();
		} catch (Exception e) {
		}
	}

	@SuppressWarnings("serial")
	public class ConnectionException extends Exception {
		public ConnectionException(String message) {
			super(message);
		}
	}
}
