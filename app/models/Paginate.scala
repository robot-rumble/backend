package models

object Paginate {
  def computeNumPages(count: Long, numPerPage: Int): Long = {
    // so that when there's a round number of entries, eg, 15 battles and 5 per page
    // we still get 3 pages, not 4
    if (count % numPerPage == 0) {
      count / numPerPage - 1
    } else {
      count / numPerPage
    }
  }
}
