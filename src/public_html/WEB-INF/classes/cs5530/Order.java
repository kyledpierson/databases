package cs5530;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;

public class Order
{
	public Order()
	{
	}
	
	/**
	 * Register a new user
	 * 
	 * @param info
	 * @return The newly issued card id, or -1 if failed
	 */
	public String register(String[] info, Connector con)
	{
		ResultSet rs = null;
		try
		{
			// Insert the user
			String query = "INSERT INTO users (username,fullname,address,city,state,zip,phone,email)"
					+ " VALUES (?,?,?,?,?,?,?,?)";
					
			con.stmt = con.conn
					.prepareStatement(query);

			con.stmt.setString(1, info[0]);
			con.stmt.setString(2, info[1]);
			con.stmt.setString(3, info[2]);
			con.stmt.setString(4, info[3]);
			con.stmt.setString(5, info[4]);
			con.stmt.setString(6, info[5]);
			con.stmt.setString(7, info[6]);
			con.stmt.setString(8, info[7]);

			con.stmt.executeUpdate();

			// Get the card id
			con.conn.commit();

			return "The user has been successfully registered";
		}
		catch (Exception e)
		{
			try
			{
				con.conn.rollback();
			}
			catch (Exception exc)
			{
			}
			return "User registration failed: " + e.getMessage() + "<br>";
		}
		finally
		{
			try
			{
				rs.close();
			}
			catch (Exception e)
			{
			}
		}
	}

	/**
	 * Checks a book out from the library
	 * 
	 * @param card_id
	 * @param book
	 */
	public String checkout(String username, String book, Connector con)
	{
		ResultSet rs = null;
		ResultSet rs2 = null;
		ResultSet rs3 = null;
		try
		{
			int card_id = 0;
			// Find the book in the library
			String query = "SELECT card_id FROM users "
					+ "WHERE username = ?";
			con.stmt = con.conn.prepareStatement(query);

			con.stmt.setString(1, username);
			rs3 = con.stmt.executeQuery();

			if (!rs3.next())
			{
				return "That is not a valid username";
			}
			else
			{
				card_id = rs3.getInt(1);
			}
			
			// Find the book in the library
			query = "SELECT books.isbn, book_id FROM books NATURAL JOIN book_copies "
					+ "WHERE (books.isbn = ? OR title = ?) AND in_stock = 1";
			con.stmt = con.conn.prepareStatement(query);

			con.stmt.setString(1, book);
			con.stmt.setString(2, book);
			rs = con.stmt.executeQuery();

			if (!rs.next())
			{
				return "That book is not in stock";
			}

			String isbn = rs.getString(1);
			int book_id = rs.getInt(2);

			// Find out if others are waiting for this book
			query = "SELECT card_id FROM requests WHERE isbn = ? AND fulfilled = 0"
					+ " ORDER BY date_requested ASC";
			con.stmt = con.conn.prepareStatement(query);

			con.stmt.setString(1, isbn);
			rs2 = con.stmt.executeQuery();

			if (rs2.next() && rs2.getInt(1) != card_id)
			{
				return "You are not first on the wait list for that book";
			}

			// If this user is waiting, the request is fulfilled
			query = "UPDATE requests SET fulfilled = 1 WHERE isbn = ? AND card_id = ?";
			con.stmt = con.conn.prepareStatement(query);
			con.stmt.setString(1, isbn);
			con.stmt.setInt(2, card_id);

			con.stmt.executeUpdate();

			// This book is no longer in stock
			query = "UPDATE book_copies SET in_stock = 0 WHERE book_id = ?";
			con.stmt = con.conn.prepareStatement(query);

			con.stmt.setInt(1, book_id);
			con.stmt.executeUpdate();

			// Finally, add this to the checkout table
			query = "INSERT INTO checkouts (book_id, card_id, check_date, due_date)"
					+ "VALUES (?, ?, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 30 DAY))";
			con.stmt = con.conn
					.prepareStatement(query);

			con.stmt.setInt(1, book_id);
			con.stmt.setInt(2, card_id);

			con.stmt.executeUpdate();

			Calendar c = Calendar.getInstance();
			c.setTime(new Date());
			c.add(Calendar.DATE, 30);

			SimpleDateFormat date_format = new SimpleDateFormat("MM/dd/yyyy");
			String due_date = date_format.format(c.getTime());

			con.conn.commit();
			return "The book id of your checkout is " + book_id + "<br>"
				   + "Your book is due on " + due_date;
		}
		catch (Exception e)
		{
			try
			{
				con.conn.rollback();
			}
			catch (SQLException exc)
			{
			}
			return "Checkout failed: " + e.getMessage() + "<br>";
		}
		finally
		{
			try
			{
				rs.close();
			}
			catch (Exception e)
			{
			}
			try
			{
				rs2.close();
			}
			catch (Exception e)
			{
			}
			try
			{
				rs3.close();
			}
			catch (Exception e)
			{
			}
		}
	}

	/**
	 * Returns a book from the specified user
	 * If the user has more than one copy of the same book, returns the one with
	 * the
	 * earliest due date
	 * 
	 * @param card_id
	 * @param book
	 */
	public String return_book(String username, String book, Connector con)
	{
		ResultSet rs = null;
		ResultSet rs2 = null;
		ResultSet rs3 = null;
		try
		{
			int card_id = 0;
			// Find the book in the library
			String query = "SELECT card_id FROM users "
					+ "WHERE username = ?";
			con.stmt = con.conn.prepareStatement(query);

			con.stmt.setString(1, username);
			rs3 = con.stmt.executeQuery();

			if (!rs3.next())
			{
				return "That is not a valid username";
			}
			else
			{
				card_id = rs3.getInt(1);
			}
			
			// Find the book that the user is returning
			query = "SELECT check_id, checkouts.book_id, books.isbn FROM"
					+ " (checkouts NATURAL JOIN book_copies) NATURAL JOIN books"
					+ " WHERE card_id = ? AND (books.isbn = ? OR title = ?) AND check_id NOT IN"
					+ " (SELECT check_id FROM returned) AND check_id NOT IN"
					+ " (SELECT check_id FROM lost)"
					+ " ORDER BY check_date ASC";
			con.stmt = con.conn.prepareStatement(query);

			con.stmt.setInt(1, card_id);
			con.stmt.setString(2, book);
			con.stmt.setString(3, book);
			rs = con.stmt.executeQuery();

			if (!rs.next())
			{
				return "You do not have a book checked-out with that isbn/title";
			}

			int check_id = rs.getInt(1);
			int book_id = rs.getInt(2);
			String isbn = rs.getString(3);

			// The book is not back in stock
			query = "UPDATE book_copies SET in_stock = 1 WHERE book_id = ?";
			con.stmt = con.conn.prepareStatement(query);
			con.stmt.setInt(1, book_id);

			con.stmt.executeUpdate();

			// Enter a row into the returned table
			query = "INSERT INTO returned (check_id, date_returned) VALUES (?, CURDATE())";
			con.stmt = con.conn.prepareStatement(query);
			con.stmt.setInt(1, check_id);

			con.stmt.executeUpdate();

			String returnVal = "Your book has been returned<br><br>";

			// Get the names of all those waiting for the recently returned book
			query = "SELECT fullname, username, users.card_id FROM requests NATURAL JOIN users WHERE isbn = ? AND fulfilled = 0 ORDER BY date_requested ASC";
			con.stmt = con.conn.prepareStatement(query);
			con.stmt.setString(1, isbn);
			rs2 = con.stmt.executeQuery();

			returnVal += "The following users are on the wait list for the returned book<br>";

			int i = 1;
			while (rs2.next())
			{
				returnVal += i + ". " + rs2.getString(1) + " (username: "
								+ rs2.getString(2) + " card id: "
								+ rs2.getInt(3) + ")<br>";
				i++;
			}
			con.conn.commit();
			return returnVal + "<br>";
		}
		catch (Exception e)
		{
			try
			{
				con.conn.rollback();
			}
			catch (SQLException exc)
			{
			}
			return "Return failed: " + e.getMessage() + "<br>";
		}
		finally
		{
			try
			{
				rs.close();
			}
			catch (Exception e)
			{
			}
			try
			{
				rs2.close();
			}
			catch (Exception e)
			{
			}
			try
			{
				rs3.close();
			}
			catch (Exception e)
			{
			}
		}
	}

	/**
	 * Submits a request from the specified user for the specified book
	 * 
	 * @param card_id
	 * @param book
	 */
	public String request(String username, String book, Connector con)
	{
		ResultSet rs = null;
		ResultSet rs2 = null;
		ResultSet rs3 = null;
		try
		{
			int card_id = 0;
			// Find the book in the library
			String query = "SELECT card_id FROM users "
					+ "WHERE username = ?";
			con.stmt = con.conn.prepareStatement(query);

			con.stmt.setString(1, username);
			rs3 = con.stmt.executeQuery();

			if (!rs3.next())
			{
				return "That is not a valid username";
			}
			else
			{
				card_id = rs3.getInt(1);
			}
			
			// Make sure the book exists
			query = "SELECT isbn FROM books WHERE isbn = ? OR title = ?";
			con.stmt = con.conn.prepareStatement(query);
			con.stmt.setString(1, book);
			con.stmt.setString(2, book);
			rs = con.stmt.executeQuery();

			// Doesn't exist
			if (!rs.next())
			{
				return "That book does not exist in our library";
			}

			String isbn = rs.getString(1);

			// Get the number of people in front of this person for the same
			// book
			query = "SELECT COUNT(*) FROM requests WHERE isbn = ? AND fulfilled = 0";
			con.stmt = con.conn.prepareStatement(query);
			con.stmt.setString(1, isbn);
			rs2 = con.stmt.executeQuery();

			int count = 1;
			if (rs2.next())
				count += rs2.getInt(1);

			query = "INSERT INTO requests (isbn, card_id, date_requested, fulfilled)"
					+ " VALUES (?, ?, NOW(), 0)";
			con.stmt = con.conn.prepareStatement(query);
			con.stmt.setString(1, isbn);
			con.stmt.setInt(2, card_id);

			con.stmt.executeUpdate();

			con.conn.commit();
			return "Your request was submitted, you are number "
					+ count + " on the waiting list<br>";
		}
		catch (Exception e)
		{
			try
			{
				con.conn.rollback();
			}
			catch (SQLException exc)
			{
			}
			return "Request failed: " + e.getMessage() + "<br>";
		}
		finally
		{
			try
			{
				rs.close();
			}
			catch (Exception e)
			{
			}
			try
			{
				rs2.close();
			}
			catch (Exception e)
			{
			}
			try
			{
				rs3.close();
			}
			catch (Exception e)
			{
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
	 */
	public String review(String username, String book, int score, String review, Connector con)
	{
		ResultSet rs = null;
		ResultSet rs2 = null;
		try
		{
			int card_id = 0;
			// Find the book in the library
			String query = "SELECT card_id FROM users "
					+ "WHERE username = ?";
			con.stmt = con.conn.prepareStatement(query);

			con.stmt.setString(1, username);
			rs2 = con.stmt.executeQuery();

			if (!rs2.next())
			{
				return "That is not a valid username";
			}
			else
			{
				card_id = rs2.getInt(1);
			}
			
			// Make sure it exists
			query = "SELECT isbn FROM books WHERE isbn = ? OR title = ?";
			con.stmt = con.conn.prepareStatement(query);
			con.stmt.setString(1, book);
			con.stmt.setString(2, book);
			rs = con.stmt.executeQuery();

			if (!rs.next())
			{
				return "That book does not exist in our library";
			}

			String isbn = rs.getString(1);

			query = "INSERT INTO reviews (card_id, isbn, score, review) VALUES (?, ?, ?, ?)";
			con.stmt = con.conn.prepareStatement(query);
			con.stmt.setInt(1, card_id);
			con.stmt.setString(2, isbn);
			con.stmt.setInt(3, score);
			con.stmt.setString(4, review);
			con.stmt.executeUpdate();

			con.conn.commit();
			return "Your review was submitted";
		}
		catch (Exception e)
		{
			try
			{
				con.conn.rollback();
			}
			catch (SQLException exc)
			{
			}
			return "Review failed: " + e.getMessage() + "<br>";
		}
		finally
		{
			try
			{
				rs.close();
			}
			catch (Exception e)
			{
			}
			try
			{
				rs2.close();
			}
			catch (Exception e)
			{
			}
		}
	}

	/**
	 * Reports a book as lost from the specified user
	 * 
	 * @param card_id
	 * @param book
	 */
	public String lost(String username, String book, Connector con)
	{
		ResultSet rs = null;
		ResultSet rs2 = null;
		try
		{
			int card_id = 0;
			// Find the book in the library
			String query = "SELECT card_id FROM users "
					+ "WHERE username = ?";
			con.stmt = con.conn.prepareStatement(query);

			con.stmt.setString(1, username);
			rs2 = con.stmt.executeQuery();

			if (!rs2.next())
			{
				return "That is not a valid username";
			}
			else
			{
				card_id = rs2.getInt(1);
			}
			
			// Find the book reported as lost
			query = "SELECT check_id, checkouts.book_id FROM"
					+ " (checkouts NATURAL JOIN book_copies) NATURAL JOIN books"
					+ " WHERE card_id = ? AND (books.isbn = ? OR title = ?)"
					+ " AND check_id NOT IN (SELECT check_id FROM returned)"
					+ " AND check_id NOT IN (SELECT check_id FROM lost)"
					+ " ORDER BY check_date ASC";
			con.stmt = con.conn.prepareStatement(query);

			con.stmt.setInt(1, card_id);
			con.stmt.setString(2, book);
			con.stmt.setString(3, book);
			rs = con.stmt.executeQuery();

			if (!rs.next())
			{
				return "You do not have a book currently checked out with that isbn/title";
			}

			int check_id = rs.getInt(1);

			// Report as lost
			query = "INSERT INTO lost (check_id, date_lost) VALUES (?, CURDATE())";
			con.stmt = con.conn.prepareStatement(query);
			con.stmt.setInt(1, check_id);

			con.stmt.executeUpdate();

			con.conn.commit();
			return "Your book has been recorded as lost";
		}
		catch (Exception e)
		{
			try
			{
				con.conn.rollback();
			}
			catch (SQLException exc)
			{
			}
			return "Report failed: " + e.getMessage() + "<br>";
		}
		finally
		{
			try
			{
				rs.close();
			}
			catch (Exception e)
			{
			}
			try
			{
				rs2.close();
			}
			catch (Exception e)
			{
			}
		}
	}

	/**
	 * Adds the book info to the library
	 * 
	 * @param info
	 */
	public String add_book(String[] info, String[] authors, Connector con)
	{
		try
		{
			String query = "INSERT INTO books (isbn,title,publisher,year,subject,summary,format)"
					+ " VALUES (?,?,?,?,?,?,?)";
			con.stmt = con.conn.prepareStatement(query);

			con.stmt.setString(1, info[0]);
			con.stmt.setString(2, info[1]);
			con.stmt.setString(3, info[2]);
			con.stmt.setString(4, info[3]);
			con.stmt.setString(5, info[4]);
			con.stmt.setString(6, info[5]);
			con.stmt.setString(7, info[6]);

			con.stmt.executeUpdate();

			// Add author to the author list
			for (String s : authors)
			{
				query = "INSERT INTO authors (a_name, isbn)" + " VALUES (?,?)";
				con.stmt = con.conn.prepareStatement(query);
				con.stmt.setString(1, s);
				con.stmt.setString(2, info[0]);

				con.stmt.executeUpdate();
			}
			con.conn.commit();
			return "Book addition succeeded<br>";
		}
		catch (Exception e)
		{
			try
			{
				con.conn.rollback();
			}
			catch (SQLException exc)
			{
			}
			return "Book addition failed: " + e.getMessage() + "<br>Perhaps that book already existed<br>";
		}
	}

	/**
	 * Adds (num) copies of the book to the library
	 * 
	 * @param info
	 * @param num
	 */
	public String add_copies(String[] info, int num, Connector con)
	{
		try
		{
			for (int i = 0; i < num; i++)
			{
				String query = "INSERT INTO book_copies (isbn,location,in_stock)"
						+ " VALUES (?,?,1)";
				con.stmt = con.conn.prepareStatement(query);

				con.stmt.setString(1, info[0]);
				con.stmt.setString(2, info[1]);

				con.stmt.executeUpdate();
			}

			con.conn.commit();
			return "Book copy addition succeeded<br>";
		}
		catch (Exception e)
		{
			try
			{
				con.conn.rollback();
			}
			catch (SQLException exc)
			{
			}
			return "Book copy addition failed: " + e.getMessage() + "<br>";
		}
	}

	/**
	 * Prints the user record for the specified user
	 * 
	 * @param card_id
	 */
	public String userRecord(String username, Connector con)
	{
		ResultSet rs = null;
		ResultSet rs2 = null;
		ResultSet rs3 = null;
		ResultSet rs4 = null;
		ResultSet rs5 = null;
		ResultSet rs6 = null;
		ResultSet rs7 = null;
		try
		{
			int card_id = 0;
			// Find the book in the library
			String query = "SELECT card_id FROM users "
					+ "WHERE username = ?";
			con.stmt = con.conn.prepareStatement(query);

			con.stmt.setString(1, username);
			rs7 = con.stmt.executeQuery();

			if (!rs7.next())
			{
				return "That is not a valid username";
			}
			else
			{
				card_id = rs7.getInt(1);
			}
			
			// Get the user info based on card id
			query = "SELECT * FROM users WHERE card_id = ?";
			con.stmt = con.conn.prepareStatement(query);
			con.stmt.setInt(1, card_id);

			rs = con.stmt.executeQuery();
			String returnVal = "";
			if (rs.next())
			{
				returnVal += "<table id=\"border\"><tr><th>Name</th><th>Username</th><th>Street</th><th>City</th><th>State</th>"
							+ "<th>Zip</th><th>Phone</th><th>Email</th></tr>"
							+ "<tr><td>" + rs.getString(3) + "</td><td>" + rs.getString(2) + "</td>"
							+ "<td>" + rs.getString(4) + "</td><td>" + rs.getString(5) + "</td>"
							+ "<td>" + rs.getString(6) + "</td><td>" + rs.getString(7) + "</td>"
							+ "<td>" + rs.getString(8) + "</td><td>" + rs.getString(9) + "</td></tr></table>";
			}
			else
			{
				return "That user does not exist";
			}
			
			returnVal += "<br>";

			// Get the currently checked out books
			query = "SELECT books.isbn, title, check_date, due_date FROM (checkouts NATURAL JOIN book_copies) NATURAL JOIN books"
					+ " WHERE card_id = ? AND check_id NOT IN"
					+ " (SELECT check_id FROM returned) AND check_id NOT IN"
					+ " (SELECT check_id FROM lost)";
			con.stmt = con.conn.prepareStatement(query);
			con.stmt.setInt(1, card_id);

			rs2 = con.stmt.executeQuery();
			returnVal += "Books currently checked-out by this user<br>"
						+ "<table id=\"border\"><tr><th>ISBN</th><th>Title</th><th>Checkout-Date</th><th>Due-Date</th></tr>";
			while (rs2.next())
			{
				returnVal += "<tr><td>" + rs2.getString(1) + "</td><td>" + rs2.getString(2) + "</td>"
							+ "<td>" + rs2.getString(3) + "</td><td>" + rs2.getString(4) + "</td></tr>";
			}
			returnVal += "</table>";

			// Get returned books
			query = "SELECT books.isbn, title, check_date, date_returned FROM"
					+ " ((checkouts NATURAL JOIN book_copies) NATURAL JOIN books)"
					+ " NATURAl JOIN returned WHERE card_id = ?";
			con.stmt = con.conn.prepareStatement(query);
			con.stmt.setInt(1, card_id);

			returnVal += "<br>";
			rs3 = con.stmt.executeQuery();
			returnVal += "Books returned out by this user<br>"
						+ "<table id=\"border\"><tr><th>ISBN</th><th>Title</th><th>Checkout-Date</th><th>Return-Date</th></tr>";
			while (rs3.next())
			{
				returnVal += "<tr><td>" + rs3.getString(1) + "</td><td>" + rs3.getString(2) + "</td>"
							+ "<td>" + rs3.getString(3) + "</td><td>" + rs3.getString(4) + "</td></tr>";
			}
			returnVal += "</table>";

			// Get lost books
			query = "SELECT books.isbn, title, check_date, date_lost FROM"
					+ " ((checkouts NATURAL JOIN book_copies) NATURAL JOIN books)"
					+ " NATURAL JOIN lost WHERE card_id = ?";
			con.stmt = con.conn.prepareStatement(query);
			con.stmt.setInt(1, card_id);

			returnVal += "<br>";
			rs4 = con.stmt.executeQuery();
			returnVal += "Books lost out by this user<br>"
						+ "<table id=\"border\"><tr><th>ISBN</th><th>Title</th><th>Checkout-Date</th><th>Date-Lost</th></tr>";
			while (rs4.next())
			{
				returnVal += "<tr><td>" + rs4.getString(1) + "</td><td>" + rs4.getString(2) + "</td>"
							+ "<td>" + rs4.getString(3) + "</td><td>" + rs4.getString(4) + "</td></tr>";
			}
			returnVal += "</table>";

			// Get book requests from this user
			query = "SELECT books.isbn, title, date_requested FROM books NATURAL JOIN requests"
					+ " WHERE card_id = ? AND fulfilled = 0";
			con.stmt = con.conn.prepareStatement(query);
			con.stmt.setInt(1, card_id);

			returnVal += "<br>";
			rs5 = con.stmt.executeQuery();
			returnVal += "Pending book requests from this user<br>"
						+ "<table id=\"border\"><tr><th>ISBN</th><th>Title</th><th>Request Date</th></tr>";
			while (rs5.next())
			{
				returnVal += "<tr><td>" + rs5.getString(1) + "</td><td>" + rs5.getString(2) + "</td>"
							+ "<td>" + rs5.getString(3) + "</td></tr>";
			}
			returnVal += "</table>";

			// Get reviews submitted by the user
			query = "SELECT books.isbn, title, score, review FROM books NATURAL JOIN reviews WHERE card_id = ?";
			con.stmt = con.conn.prepareStatement(query);
			con.stmt.setInt(1, card_id);

			returnVal += "<br>";
			rs6 = con.stmt.executeQuery();
			returnVal += "Book reviews submitted by this user<br>"
						+ "<table id=\"border\"><tr><th>ISBN</th><th>Title</th><th>Score</th><th>Review</th></tr>";
			while (rs6.next())
			{
				returnVal += "<tr><td>" + rs6.getString(1) + "</td><td>" + rs6.getString(2) + "</td>"
							+ "<td>" + rs6.getString(3) + "</td><td>" + rs6.getString(4) + "</td></tr>";
			}
			returnVal += "</table>";
			
			return returnVal;
		}
		catch (Exception e)
		{
			return "User info query failed: " + e.getMessage() + "<br>";
		}
		finally
		{
			try
			{
				rs.close();
			}
			catch (Exception e)
			{
			}
			try
			{
				rs2.close();
			}
			catch (Exception e)
			{
			}
			try
			{
				rs3.close();
			}
			catch (Exception e)
			{
			}
			try
			{
				rs4.close();
			}
			catch (Exception e)
			{
			}
			try
			{
				rs5.close();
			}
			catch (Exception e)
			{
			}
			try
			{
				rs6.close();
			}
			catch (Exception e)
			{
			}
			try
			{
				rs7.close();
			}
			catch (Exception e)
			{
			}
		}
	}

	/**
	 * When given a date, prints a list of all books that are late
	 * 
	 * @param date
	 */
	public String getLate(String sdate, Connector con)
	{
		ResultSet rs = null;
		try
		{
			SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
			Date date = format.parse(sdate);
			
			String query = "SELECT books.isbn, title, due_date, fullname, phone, email FROM"
					+ " ((checkouts NATURAL JOIN book_copies) NATURAL JOIN books) NATURAL JOIN users"
					+ " WHERE check_id NOT IN"
					+ " (SELECT check_id FROM returned) AND check_id NOT IN"
					+ " (SELECT check_id FROM lost)";
			con.stmt = con.conn.prepareStatement(query);
			rs = con.stmt.executeQuery();

			String returnVal = "Listing all books late after " + date.toString() + "<br>"
					+ "<table id=\"border\"><tr><th>ISBN</th><th>Title</th><th>Due Date</th><th>User</th><th>Phone</th><th>Email</th></tr>";
			while (rs.next())
			{
				if (date.after(rs.getDate(3)))
				{
					returnVal += "<tr><td>" + rs.getString(1) + "</td><td>" + rs.getString(2) + "</td>"
							+ "<td>" + rs.getString(3) + "</td><td>" + rs.getString(4) + "</td>"
							+ "<td>" + rs.getString(5) + "</td><td>" + rs.getString(6) + "</td></tr>";
				}
			}
			returnVal += "</table>";
			return returnVal;
		}
		catch (Exception e)
		{
			return "Late book query failed: " + e.getMessage() + "<br>";
		}
		finally
		{
			try
			{
				rs.close();
			}
			catch (Exception e)
			{
			}
		}
	}

	/**
	 * Searches the library for specified keywords
	 * option 0 = all books, 1 = all available books
	 * sort 0 = sort by year, 1 = sort by average score, 2 = sort by popularity
	 * 
	 * @param author
	 * @param publisher
	 * @param title
	 * @param subject
	 * @param option
	 * @param sort
	 */
	public String browseBooks(String author, String publisher, String title,
			String subject, int option, int sort, Connector con)
	{
		ResultSet rs = null;
		ResultSet rs2 = null;
		try
		{
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

			query += " WHERE publisher LIKE '%" + publisher
					+ "%' AND title LIKE '%" + title + "%' AND subject LIKE '%"
					+ subject + "%' AND books.isbn IN"
					+ " (SELECT isbn FROM authors WHERE a_name LIKE '%"
					+ author + "%')";

			query += " GROUP BY books.isbn";

			if (sort == 0)
				query += " ORDER BY year DESC";
			else if (sort == 1)
				query += " ORDER BY scores DESC";
			else
				query += " ORDER BY count DESC";
			
			con.stmt = con.conn.prepareStatement(query);
			rs = con.stmt.executeQuery();

			String s;
			if (sort == 0)
				s = "Year";
			else if (sort == 1)
				s = "Average Score";
			else
				s = "Number of Checkouts";

			String returnVal = "<table id=\"border\"><tr><th>ISBN</th><th>Title</th><th>Author(s)</th>"
					+ "<th>Publisher</th><th>Subject</th><th>" + s + "</th></tr>";
			
			while (rs.next())
			{
				if(option == 1 && rs.getInt(6) == 0)
					continue;
				
				query = "SELECT a_name FROM authors WHERE isbn = ?";
				con.stmt = con.conn.prepareStatement(query);
				con.stmt.setString(1, rs.getString(1));
				rs2 = con.stmt.executeQuery();

				String authors = "";
				if (rs2.next())
					authors = rs2.getString(1);

				while (rs2.next())
					authors += ", " + rs2.getString(1);

				returnVal += "<tr><td>" + rs.getString(1) + "</td><td>" + rs.getString(2) + "</td><td>" + authors + "</td>"
						+ "<td>" + rs.getString(3) + "</td><td>" + rs.getString(4) + "</td><td>" + rs.getString(5) + "</td></tr>";
			}
			returnVal += "</table>";
			return returnVal;
		}
		catch (Exception e)
		{
			return "Book browsing failed: " + e.getMessage() + "<br>";
		}
		finally
		{
			try
			{
				rs.close();
			}
			catch (Exception e)
			{
			}
			try
			{
				rs2.close();
			}
			catch (Exception e)
			{
			}
		}
	}

	public String book_info(String book, Connector con)
	{
		ResultSet rs = null;
		ResultSet rs2 = null;
		ResultSet rs3 = null;
		ResultSet rs4 = null;
		ResultSet rs5 = null;
		ResultSet rs6 = null;
		ResultSet rs7 = null;

		try
		{
			// Find the book
			String query = "SELECT books.isbn, title, publisher, subject, year, summary, format, a_name"
					+ " FROM books NATURAL JOIN authors WHERE books.isbn = ? OR title = ?";
			con.stmt = con.conn.prepareStatement(query);
			con.stmt.setString(1, book);
			con.stmt.setString(2, book);
			rs = con.stmt.executeQuery();

			if (!rs.next())
			{
				return "That book does not exist in the library<br>";
			}

			String isbn = rs.getString(1);
			String title = rs.getString(2);
			String publisher = rs.getString(3);
			String subject = rs.getString(4);
			String year = rs.getString(5);
			String summary = rs.getString(6);
			String format = rs.getString(7);
			String authors = rs.getString(8);
			
			String returnVal = "<table id=\"border\"><tr><th>ISBN</th><th>Title</th><th>Author(s)</th><th>Publisher</th>"
					+ "<th>Year</th><th>Subject</th><th>Summary</th><th>Format</th></tr>";
			while (rs.next())
			{
				authors += ", " + rs.getString(8);
			}

			returnVal += "<tr><td>" + isbn + "</td><td>" + title + "</td><td>" + authors + "</td><td>" + publisher + "</td>"
					+ "<td>" + year + "</td><td>" + subject + "</td><td>" + summary + "</td><td>" + format + "</td></tr>"
					+ "</table>";

			String table_one = "<table id=\"border\"><tr><th>Book ID</th><th>Location</th></tr>";
			String table_two = "<table id=\"border\"><tr><th>Book ID</th><th>User</th><th>Checkout-Date</th>"
					+ "<th>Date-Returned</th><th>Date-Lost</th></tr>";

			query = "SELECT book_id, location FROM book_copies WHERE isbn = ? AND in_stock = 1";
			con.stmt = con.conn.prepareStatement(query);
			con.stmt.setString(1, isbn);
			rs2 = con.stmt.executeQuery();

			while (rs2.next())
			{
				table_one += "<tr><td>" + rs2.getInt(1) + "</td><td>" + rs2.getString(2) + "</td></tr>";
			}

			query = "SELECT book_id, fullname, check_date, date_returned FROM"
					+ " ((book_copies NATURAL JOIN checkouts) NATURAL JOIN returned)"
					+ " NATURAL JOIN users WHERE isbn = ?";
			con.stmt = con.conn.prepareStatement(query);
			con.stmt.setString(1, isbn);
			rs3 = con.stmt.executeQuery();

			while (rs3.next())
			{
				table_two += "<tr><td>" + rs3.getInt(1) + "</td><td>" + rs3.getString(2) + "</td>"
						+ "<td>" + rs3.getString(3) + "</td><td>" + rs3.getString(4) + "</td><td></td></tr>";
			}

			query = "SELECT book_id, fullname, check_date, date_lost FROM"
					+ " ((book_copies NATURAL JOIN checkouts) NATURAL JOIN lost)"
					+ " NATURAL JOIN users WHERE isbn = ?";
			con.stmt = con.conn.prepareStatement(query);
			con.stmt.setString(1, isbn);
			rs4 = con.stmt.executeQuery();

			while (rs4.next())
			{
				table_one += "<tr><td>" + rs4.getInt(1) + "</td><td>lost by " + rs4.getString(2) + "</td></tr>";
				table_two += "<tr><td>" + rs4.getInt(1) + "</td><td>" + rs4.getString(2) + "</td>"
						+ "<td>" + rs4.getString(3) + "</td><td></td><td>" + rs4.getString(4) + "</td></tr>";
			}

			query = "SELECT book_id, fullname, check_date FROM (book_copies NATURAL JOIN checkouts)"
					+ " NATURAL JOIN users"
					+ " WHERE isbn = ? AND in_stock = 0 AND check_id NOT IN"
					+ " (SELECT check_id FROM lost) AND check_id NOT IN"
					+ " (SELECT check_id FROM returned)";

			con.stmt = con.conn.prepareStatement(query);
			con.stmt.setString(1, isbn);
			rs5 = con.stmt.executeQuery();

			while (rs5.next())
			{
				table_one += "<tr><td>" + rs5.getInt(1) + "</td><td>checked-out by " + rs5.getString(2) + "</td></tr>";
				table_two += "<tr><td>" + rs5.getInt(1) + "</td><td>" + rs5.getString(2) + "</td>"
						+ "<td>" + rs5.getString(3) + "</td><td></td><td></td></tr>";
			}

			table_one += "</table>";
			table_two += "</table>";
			
			returnVal += "<br>" + table_one + "<br>" + table_two + "<br>";

			query = "SELECT AVG(score) as score FROM reviews WHERE isbn = ?";
			con.stmt = con.conn.prepareStatement(query);
			con.stmt.setString(1, isbn);
			rs6 = con.stmt.executeQuery();

			if (rs6.next())
				returnVal += "Average review score: " + rs6.getDouble(1) + "<br>";
			else
				returnVal += "Average review score: 0 (No reviews)<br>";

			query = "SELECT review FROM reviews WHERE isbn = ?";
			con.stmt = con.conn.prepareStatement(query);
			con.stmt.setString(1, isbn);
			rs7 = con.stmt.executeQuery();

			returnVal += "Reviews of this book<br>";
			int count = 1;
			while (rs7.next())
			{
				returnVal += count + ". " + rs7.getString(1) + "<br>";
				count++;
			}
			
			return returnVal;
		}
		catch (Exception e)
		{
			return "Book info failed: " + e.getMessage() + "<br>";
		}
		finally
		{
			try
			{
				rs.close();
			}
			catch (Exception e)
			{
			}
			try
			{
				rs2.close();
			}
			catch (Exception e)
			{
			}
			try
			{
				rs3.close();
			}
			catch (Exception e)
			{
			}
			try
			{
				rs4.close();
			}
			catch (Exception e)
			{
			}
			try
			{
				rs5.close();
			}
			catch (Exception e)
			{
			}
			try
			{
				rs6.close();
			}
			catch (Exception e)
			{
			}
			try
			{
				rs7.close();
			}
			catch (Exception e)
			{
			}
		}
	}

	public String checkStats(int val, Connector con)
	{
		ResultSet rs = null;
		try
		{
			String query = "SELECT title, books.isbn, COUNT(books.isbn) FROM"
					+ " (checkouts NATURAL JOIN book_copies) NATURAL JOIN books"
					+ " GROUP BY books.isbn ORDER BY COUNT(books.isbn) DESC";
			con.stmt = con.conn.prepareStatement(query);
			rs = con.stmt.executeQuery();

			int i = 0;
			String returnVal = "<table id=\"border\"><tr><th>Number</th><th>Title</th><th>ISBN</th><th>Checkouts</th></tr>";
			while (rs.next() && i < val)
			{
				returnVal += "<tr><td>" + ++i + "</td><td>" + rs.getString(1) + "</td>"
						+ "<td>" + rs.getString(2) + "</td><td>" + rs.getInt(3) + "</td></tr>";
			}
			returnVal += "</table>";
			return returnVal;
		}
		catch (Exception e)
		{
			return "Checkout stats failed: " + e.getMessage() + "<br>";
		}
		finally
		{
			try
			{
				rs.close();
			}
			catch (Exception e)
			{
			}
		}
	}

	public String reqStats(int val, Connector con)
	{
		ResultSet rs = null;
		try
		{
			String query = "SELECT title, books.isbn, COUNT(books.isbn) FROM requests NATURAL JOIN books GROUP BY books.isbn ORDER BY COUNT(books.isbn) DESC";
			con.stmt = con.conn.prepareStatement(query);
			rs = con.stmt.executeQuery();

			int i = 0;
			
			String returnVal = "<table id=\"border\"><tr><th>Number</th><th>Title</th><th>ISBN</th><th>Requests</th></tr>";
			while (rs.next() && i < val)
			{
				returnVal += "<tr><td>" + ++i + "</td><td>" + rs.getString(1) + "</td><td>" + rs.getString(2) + "</td>"
						+ "<td>" + rs.getInt(3) + "</td></tr>";
			}
			returnVal += "</table>";
			return returnVal;
		}
		catch (Exception e)
		{
			return "Requests stats failed: " + e.getMessage() + "<br>";
		}
		finally
		{
			try
			{
				rs.close();
			}
			catch (Exception e)
			{
			}
		}
	}

	public String lostStats(int val, Connector con)
	{
		ResultSet rs = null;
		try
		{
			String query = "SELECT title, books.isbn, COUNT(books.isbn) FROM ((checkouts NATURAL JOIN lost) NATURAL JOIN book_copies) NATURAL JOIN books GROUP BY books.isbn ORDER BY COUNT(books.isbn) DESC";
			con.stmt = con.conn.prepareStatement(query);
			rs = con.stmt.executeQuery();

			int i = 0;
			
			String returnVal = "<table id=\"border\"><tr><th>Number</th><th>Title</th><th>ISBN</th><th>Books Lost</th></tr>";
			while (rs.next() && i < val)
			{
				returnVal += "<tr><td>" + ++i + "</td><td>" + rs.getString(1) + "</td><td>" + rs.getString(2)
						+ "</td><td>" + rs.getInt(3) + "</td></tr>";
			}
			returnVal += "</table>";
			return returnVal;
		}
		catch (Exception e)
		{
			return "Lost stats failed: " + e.getMessage() + "<br>";
		}
		finally
		{
			try
			{
				rs.close();
			}
			catch (Exception e)
			{
			}
		}
	}

	public String authStats(int val, Connector con)
	{
		ResultSet rs = null;
		try
		{
			String query = "SELECT a_name, COUNT(a_name) FROM ((checkouts NATURAL JOIN book_copies)"
					+ " NATURAL JOIN books) INNER JOIN authors ON books.isbn = authors.isbn"
					+ " GROUP BY a_name ORDER BY COUNT(a_name) DESC";
			con.stmt = con.conn.prepareStatement(query);
			rs = con.stmt.executeQuery();

			int i = 0;
			
			String returnVal = "<table id=\"border\"><tr><th>Number</th><th>Author</th><th>Checkouts of books by this author</th></tr>";
			while (rs.next() && i < val)
			{
				returnVal += "<tr><td>" + ++i + "</td><td>" + rs.getString(1) + "</td><td>" + rs.getInt(2) + "</td></tr>";
			}
			returnVal += "</table>";
			return returnVal;
		}
		catch (Exception e)
		{
			return "Author stats failed: " + e.getMessage() + "<br>";
		}
		finally
		{
			try
			{
				rs.close();
			}
			catch (Exception e)
			{
			}
		}
	}

	public String userCheckStats(int val, Connector con)
	{
		ResultSet rs = null;
		try
		{
			String query = "SELECT fullname, COUNT(card_id) FROM checkouts NATURAL JOIN users"
					+ " GROUP BY card_id ORDER BY COUNT(card_id) DESC";
			con.stmt = con.conn.prepareStatement(query);
			rs = con.stmt.executeQuery();

			int i = 0;
			
			String returnVal = "<table id=\"border\"><tr><th>Number</th><th>User</th><th>Checkouts</th></tr>";
			while (rs.next() && i < val)
			{
				returnVal += "<tr><td>" + ++i + "</td><td>" + rs.getString(1) + "</td><td>" + rs.getInt(2) + "</td></tr>";
			}
			returnVal += "</table>";
			return returnVal;
		}
		catch (Exception e)
		{
			return "User checkout stats failed: " + e.getMessage() + "<br>";
		}
		finally
		{
			try
			{
				rs.close();
			}
			catch (Exception e)
			{
			}
		}
	}

	public String userRevStats(int val, Connector con)
	{
		ResultSet rs = null;
		try
		{
			String query = "SELECT fullname, COUNT(card_id) FROM reviews NATURAL JOIN users"
					+ " GROUP BY card_id ORDER BY COUNT(card_id) DESC";
			con.stmt = con.conn.prepareStatement(query);
			rs = con.stmt.executeQuery();

			int i = 0;

			String returnVal = "<table id=\"border\"><tr><th>Number</th><th>User</th><th>Reviews</th></tr>";
			while (rs.next() && i < val)
			{
				returnVal += "<tr><td>" + ++i + "</td><td>" + rs.getString(1) + "</td><td>" + rs.getInt(2) + "</td></tr>";
			}
			returnVal += "</table>";
			return returnVal;
		}
		catch (Exception e)
		{
			return "Lost stats failed: " + e.getMessage() + "<br>";
		}
		finally
		{
			try
			{
				rs.close();
			}
			catch (Exception e)
			{
			}
		}
	}

	public String userLoseStats(int val, Connector con)
	{
		ResultSet rs = null;
		try
		{
			String query = "SELECT fullname, COUNT(card_id) FROM (checkouts NATURAL JOIN lost)"
					+ " NATURAL JOIN users GROUP BY card_id ORDER BY COUNT(card_id) DESC";
			con.stmt = con.conn.prepareStatement(query);
			rs = con.stmt.executeQuery();

			int i = 0;
			String returnVal = "<table id=\"border\"><tr><th>Number</th><th>User</th><th>Losses</th></tr>";
			while (rs.next() && i < val)
			{
				returnVal += "<tr><td>" + ++i + "</td><td>" + rs.getString(1) + "</td>"
						+ "<td>" + rs.getInt(2) + "</td></tr>";
			}
			returnVal += "</table>";
			return returnVal;
		}
		catch (Exception e)
		{
			return "User lose stats failed: " + e.getMessage() + "<br>";
		}
		finally
		{
			try
			{
				rs.close();
			}
			catch (Exception e)
			{
			}
		}
	}
}
