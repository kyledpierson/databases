package project;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import project.LibraryConnection.ConnectionException;

public class LibraryOptions {
	static LibraryConnection lc = null;
	static Scanner in = null;

	public static void main(String[] args) {
		String filename = "database-config.txt";
		String url = "";
		String username = "";
		String password = "";

		try {
			FileReader fileReader = new FileReader(filename);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String line;

			while ((line = bufferedReader.readLine()) != null) {
				String[] tokens = line.split(" ");
				switch (tokens[0]) {
				case "url":
					url = tokens[1];
					break;
				case "username":
					username = tokens[1];
					break;
				case "password":
					password = tokens[1];
					break;
				default:
					break;
				}
			}

			bufferedReader.close();
		} catch (FileNotFoundException ex) {
			System.out.println("Unable to open file " + filename);
		} catch (IOException ex) {
			System.out.println("Error reading file " + filename);
		}

		try {
			lc = new LibraryConnection(url, username, password);
			boolean loop = true;

			// General user options
			while (loop) {
				System.out.println("Choose an number option below:");
				System.out.println("1 Register as a new user");
				System.out.println("2 Login with a username");
				System.out.println("3 Add new book information");
				System.out.println("4 Add new copies of a book to the library");
				System.out.println("5 Get a list of all late books");
				System.out.println("6 Browse books");
				System.out.println("7 Get information on a specific book");
				System.out.println("8 View book statistics");
				System.out.println("9 View user statistics");
				System.out.println("10 End program");

				in = new Scanner(System.in);
				String option = in.nextLine().trim();

				switch (option) {
				case "1":
					loop = register_user();
					break;
				case "2":
					loop = login();
					break;
				case "3":
					loop = add_book();
					break;
				case "4":
					loop = add_copies();
					break;
				case "5":
					loop = list_late_books();
					break;
				case "6":
					loop = browse_books();
					break;
				case "7":
					loop = book_info();
					break;
				case "8":
					loop = book_stats();
					break;
				case "9":
					loop = user_stats();
					break;
				case "10":
					loop = false;
					break;
				default:
					System.out.println("Please enter an integer between 1 and 10\r\n");
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		} finally {
			if (lc != null)
				lc.close();
			if (in != null)
				in.close();
		}
	}

	static boolean register_user() {
		while (true) {
			System.out.println("Please type in the following information (separated by commas):");
			System.out.println("Type \"back\" to go back or \"quit\" to quit");
			System.out.println("<username>,<full name>,<street address>,<city>,<state>,<zip>,<phone>,<email>");

			String input = in.nextLine().trim();

			if (input.equals("back"))
				return true;
			else if (input.equals("quit"))
				return false;

			String[] user_input = input.split(",");
			if (user_input.length < 8) {
				System.out.println("Please enter ALL information, separated by commas\r\n");
				continue;
			}
			String[] user_info = new String[8];

			for (int i = 0; i < 8; i++) {
				String st = user_input[i].trim();
				if (i > 0 && i < 7)
					st = st.toUpperCase();

				user_info[i] = st;
			}

			try {
				int card_id = lc.register(user_info);
				System.out.println("You have been registered. Your card id is " + card_id + "\r\n");
				return true;
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}

	static boolean login() {
		while (true) {
			System.out.println("Please enter your username:");
			System.out.println("Type \"back\" to go back or \"quit\" to quit");

			String username = in.nextLine().trim();

			if (username.equals("back"))
				return true;
			else if (username.equals("quit"))
				return false;

			try {
				int card_id = lc.login(username);
				if (card_id == -1) {
					System.out.println("That username does not exist\r\n");
					continue;
				}

				while (true) {
					System.out.println("Choose an option below");
					System.out.println("1 Check-out a book");
					System.out.println("2 Return a book");
					System.out.println("3 Request a book");
					System.out.println("4 Review a book");
					System.out.println("5 Report a book as lost");
					System.out.println("6 Full user record");
					System.out.println("7 Back to main page");
					System.out.println("8 Quit");

					String input = in.nextLine().trim();
					String book;

					switch (input) {
					case "1":
						System.out.println("Enter the ISBN or title below");
						book = in.nextLine().trim().toUpperCase();

						lc.checkout(card_id, book);
						break;
					case "2":
						System.out.println("Enter the ISBN or title below");
						book = in.nextLine().trim().toUpperCase();

						lc.return_book(card_id, book);
						break;
					case "3":
						System.out.println("Enter the ISBN or title below");
						book = in.nextLine().trim().toUpperCase();

						lc.request(card_id, book);
						break;
					case "4":
						System.out.println(
								"Enter the ISBN or title below, followed by a space and your score (1-10), followed by a colon and your review");
						String result = in.nextLine().trim();
						book = result.substring(0, result.indexOf(' ')).trim().toUpperCase();
						String string_score = result.substring(result.indexOf(' ') + 1, result.indexOf(':')).trim();
						String review = result.substring(result.indexOf(':') + 1).trim();

						int score;
						try {
							score = Integer.parseInt(string_score);
							if (score < 1 || score > 10) {
								System.out.println("Please enter a score between 1 and 10\r\n");
								break;
							}
						} catch (Exception e) {
							System.out.println("Please enter a numerical score between 1 and 10\r\n");
							break;
						}

						lc.review(card_id, book, score, review);
						break;
					case "5":
						System.out.println("Enter the ISBN or title below");
						book = in.nextLine().trim().toUpperCase();

						lc.lost(card_id, book);
						break;
					case "6":
						lc.userRecord(card_id);
						break;
					case "7":
						return true;
					case "8":
						return true;
					default:
						System.out.println("Please enter an integer between 1 and 6\r\n");
					}
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}

	static boolean add_book() {
		while (true) {
			System.out.println("Please type in the following information (separated by commas):");
			System.out.println("<isbn>,<title>,<publisher>,<year>,<subject>,<summary>,<format>,<author1>,<author2>...");
			System.out.println("Type \"back\" to go back or \"quit\" to quit");

			String input = in.nextLine().trim().toUpperCase();

			if (input.equals("BACK"))
				return true;
			else if (input.equals("QUIT"))
				return false;

			String[] book_input = input.split(",");
			if (book_input.length < 8) {
				System.out.println("Please enter ALL information, separated by commas");
				continue;
			}

			String[] book_info = new String[7];
			String[] authors = new String[book_input.length - 7];

			for (int i = 0; i < 7; i++)
				book_info[i] = book_input[i].trim();

			for (int i = 7; i < book_input.length; i++)
				authors[i - 7] = book_input[i].trim();

			try {
				lc.add_book(book_info, authors);
				System.out.println("The book has been successfully added");
				return true;
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}

	static boolean add_copies() {
		while (true) {
			System.out.println("Please type in the following information (separated by commas):");
			System.out.println("<isbn>,<location>,<num_copies>");
			System.out.println("Type \"back\" to go back or \"quit\" to quit");

			String input = in.nextLine().trim().toUpperCase();

			if (input.equals("BACK"))
				return true;
			else if (input.equals("QUIT"))
				return false;

			String[] book_input = input.split(",");
			if (book_input.length < 3) {
				System.out.println("Please enter ALL information, separated by commas");
				continue;
			}

			int num;
			try {
				num = Integer.parseInt(book_input[2].trim());
			} catch (Exception e) {
				System.out.println("Could not parse the number of copies");
				continue;
			}
			String[] book_info = new String[2];

			for (int i = 0; i < 2; i++)
				book_info[i] = book_input[i].trim();

			try {
				lc.add_copies(book_info, num);
				System.out.println("The book copies have been successfully added");
				return true;
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}

	static boolean list_late_books() {
		while (true) {
			System.out.println("Please type in a date (mm/dd/yyyy):");
			System.out.println("Type \"back\" to go back or \"quit\" to quit");

			String input = in.nextLine().trim().toUpperCase();

			if (input.equals("BACK"))
				return true;
			else if (input.equals("QUIT"))
				return false;

			Date date;
			try {
				SimpleDateFormat date_format = new SimpleDateFormat("MM/dd/yyyy");
				date = date_format.parse(input);
			} catch (Exception e) {
				System.out.println("Your date could not be parsed");
				continue;
			}

			try {
				lc.getLate(date);
			} catch (ConnectionException e) {
				System.out.println(e.getMessage());
			}

			return true;
		}
	}

	static boolean browse_books() {
		while (true) {
			System.out.println("Please enter the following information (leave blank for wildcards):");
			System.out.println(
					"<author>, <publisher>, <title>, <subject>, <only show available books (y/n)>, <sort by year(y), scores (s), popularity (p))>");
			System.out.println("Type \"back\" to go back or \"quit\" to quit");

			String input = in.nextLine().trim().toUpperCase();

			if (input.equals("BACK"))
				return true;
			else if (input.equals("QUIT"))
				return false;

			String[] user_input = input.split(",");
			if (user_input.length < 6) {
				System.out.println("Please enter ALL information, separated by commas\r\n");
				continue;
			}
			String[] user_info = new String[6];

			for (int i = 0; i < 6; i++)
				user_info[i] = user_input[i].trim();

			int option;
			if (user_info[4].equals("Y") || user_info[4].equals("YES"))
				option = 1;
			else if (user_info[4].equals("N") || user_info[4].equals("NO"))
				option = 0;
			else {
				System.out.println("You did not enter Y or N\r\n");
				System.out.println(user_info[4]);
				continue;
			}

			int sort;
			if (user_info[5].equals("Y") || user_info[5].equals("YEAR"))
				sort = 0;
			else if (user_info[5].equals("S") || user_info[5].equals("SCORES"))
				sort = 1;
			else if (user_info[5].equals("P") || user_info[5].equals("POPULARITY"))
				sort = 2;
			else {
				System.out.println("You did not enter a valid sort option\r\n");
				continue;
			}

			try {
				lc.browseBooks(user_info[0], user_info[1], user_info[2], user_info[3], option, sort);
			} catch (ConnectionException e) {
				System.out.println(e.getMessage());
			}

			return true;
		}
	}

	static boolean book_info() {
		while (true) {
			try {
				System.out.println("Enter the ISBN or title below");
				String book = in.nextLine().trim().toUpperCase();

				lc.book_info(book);

				return true;
			} catch (ConnectionException e) {
				System.out.println(e.getMessage());
			}
		}
	}

	static boolean book_stats() {
		while (true) {
			try {
				System.out.println("Choose an option below, followed by a space and how many values you want returned");
				System.out.println("1 Most checked-out books (n)");
				System.out.println("2 Most requested books (n)");
				System.out.println("3 Most lost books (n)");
				System.out.println("4 Most popular authors (n)");
				System.out.println("5 Back to main menu");
				System.out.println("6 Quit");
				String input[] = in.nextLine().trim().split(" ");

				String option = "";
				try {
					option = input[0];
				} catch (Exception e) {
					System.out.println("Your input was not valid");
					continue;
				}

				if (option.equals("5"))
					return true;
				else if (option.equals("6"))
					return false;

				int val = 0;
				try {
					String num = input[1];
					val = Integer.parseInt(num);
				} catch (Exception e) {
					System.out.println("Enter an integer for the n value\r\n");
					continue;
				}

				switch (option) {
				case "1":
					lc.checkStats(val);
					break;
				case "2":
					lc.reqStats(val);
					break;
				case "3":
					lc.lostStats(val);
					break;
				case "4":
					lc.authStats(val);
					break;
				default:
					System.out.println("Enter an integer between 1 and 6\r\n");
				}
			} catch (ConnectionException e) {
				System.out.println(e.getMessage());
			}
		}
	}

	static boolean user_stats() {
		while (true) {
			try {
				System.out.println("Choose an option below, followed by a space and how many values you want returned");
				System.out.println("1 Users that check out the most books (n)");
				System.out.println("2 Users that rate the most books (n)");
				System.out.println("3 Users that lose the most books (n)");
				System.out.println("4 Back to main menu");
				System.out.println("5 Quit");
				String input[] = in.nextLine().trim().split(" ");

				String option = "";
				try {
					option = input[0];
				} catch (Exception e) {
					System.out.println("Your input was not valid");
					continue;
				}

				if (option.equals("4"))
					return true;
				else if (option.equals("5"))
					return false;

				int val = 0;
				try {
					String num = input[1];
					val = Integer.parseInt(num);
				} catch (Exception e) {
					System.out.println("Enter an integer for the n value\r\n");
					continue;
				}

				switch (option) {
				case "1":
					lc.userCheckStats(val);
					break;
				case "2":
					lc.userRevStats(val);
					break;
				case "3":
					lc.userLoseStats(val);
					break;
				default:
					System.out.println("Enter an integer between 1 and 5\r\n");
				}
			} catch (ConnectionException e) {
				System.out.println(e.getMessage());
			}
		}
	}
}